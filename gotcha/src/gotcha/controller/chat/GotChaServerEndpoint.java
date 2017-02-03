package gotcha.controller.chat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.DecodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gotcha.model.Message;
import gotcha.globals.Globals;

@ServerEndpoint(
	value = "/{nickname}",
	decoders = MessageDecoder.class,
	encoders = MessageEncoder.class
)
public class GotChaServerEndpoint {
	
	// Track online users in the system
	private static Map<String, Session> active = Collections.synchronizedMap(new HashMap<String, Session>()); 
	
	// Decoder & Encoder
	MessageDecoder decoder = new MessageDecoder();
	MessageEncoder encoder = new MessageEncoder();
	
	/**
	 * 
	 */
	@OnOpen
	public void login (Session session, @PathParam("nickname") String user) throws IOException {
		if (session.isOpen()) {
			active.put(user, session);
			connectUser(user);
			System.out.println("The user \"" + user + "\" is now active.");
		}
	}
	
	/**
	 * 
	 */
	@OnMessage
	public void send (String jsonMessage) throws IOException {
		// Convert the retrieved message into Message object
		try {
			if (decoder.willDecode(jsonMessage)) {
				Message message = decoder.decode(jsonMessage);
				int messageId = store(message);
				message.id(messageId);
				String encodedMessage = recreate(jsonMessage, messageId);
				// The message must be sent to a channel
				if (Globals.channels.containsKey(message.to())) {
					broadcast(message.to(), encodedMessage);
				// The message must be sent to a specific user
				} else {
					if (!message.from().equals(message.to())) {
						notify(message.to(), encodedMessage);
						notify(message.from(), encodedMessage);
					} else {
						notify(message.to(), encodedMessage);
					}
				}
			} 
		} catch (DecodeException | messageDeliveryException e) {
			System.out.println("Something went wrong!");
		}	
	}

	/**
	 * 
	 */
	@OnClose
	public void logout (Session session) throws IOException {
		for (Entry<String, Session> user : active.entrySet()) {
			if (user.getValue().equals(session)) {
				logoff(user.getKey());
				active.remove(user.getKey());
			}
		}
	}
	
	@OnError
	public void log (Session session, Throwable t) {
		// Generally, this occurs on connection reset
		for (Entry<String, Session> one : active.entrySet()) {
			if (one.getValue().equals(session)) {
				System.out.println("The user \"" + one.getKey() + "\" has gone away.");
			}
		}
	}

	/**
	 * 
	 */
	private void broadcast (String channel, String jsonMessage) throws messageDeliveryException {
		ArrayList<String> subscribers = Globals.channels.get(channel);
		for (String subscriber : subscribers) {
			notify(subscriber, jsonMessage);
		}		
	}

	/**
	 * 
	 */
	private boolean notify (String user, String jsonMessage) throws messageDeliveryException {
		if (active.containsKey(user)) {
			Session session = active.get(user);
			try {
				session.getBasicRemote().sendText(jsonMessage);
			} catch (IOException e) {
				messageDeliveryException exception = new messageDeliveryException("Something went wrong, the message was not delivered to: @" + user);
				throw exception;
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 */
	private int store (Message message) {
		int messageId = 1;
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(Globals.INSERT_MESSAGE, messageId);

			statement.setInt(1, message.parentId());
			statement.setString(2, message.from());
			statement.setString(3, message.to());
			statement.setString(4, message.text());
			statement.setTimestamp(5, message.lastUpdate());
			statement.setTimestamp(6, message.time());
			
			statement.executeUpdate();
			
			ResultSet id = statement.getGeneratedKeys();
			if (id.next()) {
				messageId = id.getInt(1);
			}
			
			connection.commit();
			statement.close();
			connection.close();
			
		} catch (SQLException e ){
			System.out.println("An error has occured while trying to execute the query!");
		}
		
		return messageId;
	}
    
    private void logoff (String user) {
    	String status = "away";
		Timestamp last_seen = new Timestamp(System.currentTimeMillis());
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(Globals.UPDATE_USER_STATUS);
			
			statement.setString(1, status);
			statement.setTimestamp(2, last_seen);
			statement.setString(3, user);
			
			statement.executeUpdate();
			connection.commit();
			statement.close();
			connection.close();
			
		} catch (SQLException e) {
			System.out.println("An error has occured while trying to execute the query!");
		}
    }
    
    private void connectUser (String user) {
    	String status = "active";
		Timestamp last_seen = new Timestamp(System.currentTimeMillis());
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(Globals.UPDATE_USER_STATUS);
			
			statement.setString(1, status);
			statement.setTimestamp(2, last_seen);
			statement.setString(3, user);
			
			statement.executeUpdate();
			connection.commit();
			statement.close();
			connection.close();
			
		} catch (SQLException e) {
			System.out.println("An error has occured while trying to execute the query!");
		}
    }
    
    public String recreate (String textMessage, int id) {
    	JsonParser parser = new JsonParser();
    	JsonElement jsonMessage = parser.parse(textMessage);
    	JsonElement messageId = parser.parse("{\"id\": \"" + id + "\"}");
    	JsonObject messageIdObject = messageId.getAsJsonObject();
    	JsonObject messageObject = new JsonObject();
    	messageObject.add("message", jsonMessage);
    	messageObject = messageObject.getAsJsonObject("message");
    	messageObject.add("id", messageIdObject.get("id"));
    	return messageObject.toString();
    }
}