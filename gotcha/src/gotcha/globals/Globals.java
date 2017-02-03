package gotcha.globals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.*;

import gotcha.controller.search.GotchaSearchEngine;
import gotcha.model.Channel;

public final class Globals {
	public static final String dbName = "gotchaDB";
	public static Context context = null;
	public static Database database;
	// System search engine instantiation
	public static GotchaSearchEngine searchEngine;
	// Public system properties
	public static HashMap<String, ArrayList<String>> channels = new HashMap<String, ArrayList<String>>();

	// Public variables and statements for SQL queries
	// USERS TABLE STATEMENTS
	public static final String SELECT_ALL_USERS = "SELECT * FROM USERS";
	public static final String SELECT_USER_BY_USERNAME = "SELECT * FROM USERS WHERE USERNAME=?";
	public static final String SELECT_USER_BY_NICKNAME = "SELECT * FROM USERS WHERE NICKNAME=?";
	public static final String SELECT_USER_BY_USERNAME_OR_NICKNAME = "SELECT * FROM USERS WHERE USERNAME=? OR NICKNAME=?";
	public static final String SELECT_USER_BY_USERNAME_AND_PASSWORD = "SELECT * FROM USERS WHERE USERNAME=? AND PASSWORD=?";
	public static final String INSERT_USER = "INSERT INTO USERS (USERNAME, PASSWORD, NICKNAME, DESCRIPTION, PHOTO_URL, STATUS, LAST_SEEN) VALUES (?,?,?,?,?,?,?)";
	public static final String UPDATE_USER_DETAILS = "UPDATE USERS SET USERNAME=?, PASSWORD=?, NICKNAME=?, DESCRIPTION=?, PHOTOURL=?, STATUS=?, LASTSEEN=? WHERE USERNAME=?";
	public static final String UPDATE_USER_STATUS = "UPDATE USERS SET STATUS=?, LAST_SEEN=? WHERE NICKNAME=?";
	public static final String LOGOFF_USERS = "UPDATE USERS SET STATUS=?, LAST_SEEN=? WHERE STATUS=?";
	
	// DIRECT_MESSAGES TABLE STATEMENTS
	public static final String SELECT_ALL_MESSAGES = "SELECT * FROM MESSAGES";
	public static final String SELECT_MESSAGE_BY_SENDER = "SELECT * FROM MESSAGES WHERE SENDER=?";
	public static final String SELECT_MESSAGE_BY_RECEIVER = "SELECT * FROM MESSAGES WHERE RECEIVER=?";
	public static final String SELECT_TEN_CHANNEL_MESSAGES = "SELECT * FROM MESSAGES WHERE PARENT_ID=0 AND RECEIVER=? AND LAST_UPDATE>=? ORDER BY LAST_UPDATE DESC OFFSET ? ROWS FETCH NEXT 10 ROWS ONLY";
	public static final String SELECT_TEN_DIRECT_MESSAGES = "SELECT * FROM MESSAGES WHERE PARENT_ID=0 AND ((RECEIVER=? AND SENDER=?) OR (RECEIVER=? AND SENDER=?)) ORDER BY LAST_UPDATE DESC OFFSET ? ROWS FETCH NEXT 10 ROWS ONLY";
	public static final String SELECT_MESSAGE_REPLIES = "SELECT * FROM MESSAGES WHERE PARENT_ID=? ORDER BY LAST_UPDATE DESC";
	public static final String SELECT_MESSAGE_BY_SENDER_AND_RECEIVER = "SELECT * FROM MESSAGES WHERE SENDER=? AND RECEIVER=?";
	public static final String INSERT_MESSAGE = "INSERT INTO MESSAGES (PARENT_ID, SENDER, RECEIVER, TEXT, LAST_UPDATE, SENT_TIME) VALUES (?,?,?,?,?,?)";
	public static final String UPDATE_MESSAGE_LAST_UPDATE_TIME = "UPDATE MESSAGES SET LAST_UPDATE=? WHERE MESSAGE_ID=?";

	// REPLIES TABLE STATEMENTS
	public static final String SELECT_ALL_MESSAGE_REPLIES = "SELECT * REPLIES WHERE MESSAGE_ID=?";
	public static final String INSERT_REPLY = "INSERT INTO REPLIES (MESSAGE_ID, SENDER, TEXT, SENT_TIME) VALUES (?,?,?,?)";

	// CHANNELS TABLE STATEMENTS
	public static final String SELECT_ALL_CHANNELS = "SELECT * FROM CHANNELS";
	public static final String SELECT_CHANNEL_BY_NAME = "SELECT * FROM CHANNELS WHERE NAME=?";
	public static final String INSERT_CHANNEL = "INSERT INTO CHANNELS (NAME, DESCRIPTION, CREATED_BY, CREATED_TIME) VALUES (?,?,?,?)";
	public static final String UPDATE_CHANNEL_DESCRIPTION = "UPDATE CHANNELS SET DESCRIPTION=? WHERE NAME=?";
	public static final String UPDATE_CHANNEL_NAME = "UPDATE CHANNELS SET NAME=? WHERE NAME=? AND CREATED_BY=?";
	public static final String DELETE_CHANNEL = "DELETE FROM CHANNELS WHERE NAME=? AND CREATED_BY=?";
	
	// SUBSCRIPTIONS TABLE STATEMENTS
	public static final String SELECT_SUBSCRIPTON_BY_USER = "SELECT * FROM SUBSCRIPTIONS WHERE NICKNAME=?";
	public static final String SELECT_SUBSCRIPTON_BY_CHANNEL = "SELECT * FROM SUBSCRIPTIONS WHERE CHANNEL=?";
	public static final String SELECT_ALL_SUBSCRIPTIONS = "SELECT * FROM SUBSCRIPTIONS";
	public static final String DELETE_SUBSCRIPTON = "DELETE FROM SUBSCRIPTIONS WHERE NICKNAME=? AND CHANNEL=?";
	public static final String INSERT_SUBSCRIPTON = "INSERT INTO SUBSCRIPTIONS (NICKNAME, CHANNEL) VALUES (?,?)";

	public static ArrayList<String> getSubscribersList (String channel) {
		ArrayList<String> subscribers = new ArrayList<String>();
		
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(SELECT_SUBSCRIPTON_BY_CHANNEL);
			statement.setString(1, channel);
			
			ResultSet resultSet = statement.executeQuery();
		
			while (resultSet.next()) {
				subscribers.add(resultSet.getString("NICKNAME"));
			}
			
			statement.close();
			connection.close();
			
		} catch (SQLException e) {
			System.out.println("An error has occurred while trying to get channel subscriptions data from database!");
		}

		return subscribers;
	}
	
	public static ArrayList<String> getAllChannels () {
		ArrayList<String> channels = new ArrayList<String>();
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(SELECT_ALL_CHANNELS);
			ResultSet resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				channels.add(resultSet.getString("NAME"));
			}
			
			statement.close();
			connection.close();
			
		} catch (SQLException e) {
			System.out.println("An error has occurred while trying to get channels data from database!");
		}

		return channels;
	}
	
	public static ArrayList<Channel> getUserSubscriptions (String nickname) {
		ArrayList<Channel> channels = new ArrayList<Channel>();
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(SELECT_SUBSCRIPTON_BY_USER);
			statement.setString(1, nickname);
			
			ResultSet resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				channels.add(getChannel(resultSet.getString("CHANNEL")));
			}
			
			statement.close();
			connection.close();
		} catch (SQLException e) {
			System.out.println("An error has occurred while trying to get user subscriptions data from database!");
		}
		
		return channels;
	}
	
	public static Channel getChannel (String name) {
		Channel channel = new Channel();
		
		try {
			Connection connection = Globals.database.getConnection();
			PreparedStatement statement = connection.prepareStatement(SELECT_CHANNEL_BY_NAME);
			statement.setString(1, name);
			
			ResultSet resultSet = statement.executeQuery();
			
			while (resultSet.next()) {
				
				channel.name(resultSet.getString("NAME"));
				channel.description(resultSet.getString("DESCRIPTION"));
			}
			
			statement.close();
			connection.close();
		} catch (SQLException e) {
			System.out.println("An error has occurred while trying to get channel data from database!");
		}
		
		return channel;
	}
}
