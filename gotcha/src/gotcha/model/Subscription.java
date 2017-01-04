package gotcha.model;

public class Subscription {
	private int id;
	private String username;
	private String channel;

	// Constructor
	public Subscription (int id, String username, String channel) {
		this.id = id;
		this.username = username;
		this.channel = channel;
	}

	public String username () {
		return this.username;
	}

	public String channel () {
		return this.channel;
	}
}
