package edu.johny.ringescape.server;

/** 
 * Class that contains information about a client 
 * @author Ioannis Chiotellis
 **/
public class Client implements AppConstants {

	protected int id;
	private String name;
	private int radius;
	private double latitude;
	private double longitude;
	private final long timeIn;
	private int gameId;
	private Protocol protocol;
	private long offset;
	private boolean timeOut;
	Utils u = new Utils();

	public Client() {
		this.name = "You";
		this.radius = 0;
		this.latitude = 0;
		this.longitude = 0;
		timeIn = System.currentTimeMillis();
	}

	public Client(Protocol protocol, String data) {
		this.id = 0;
		this.protocol = protocol;
		this.timeIn = System.currentTimeMillis();
		this.setTimeOut(false);
		String[] info = data.split(FIELD_DELIM);
		u.log("Data ="+data+"end");
		int dataLength = info.length;
		if(dataLength >= 1)
			this.name = info[0];
		else
			this.name = "Anon-Client";
		if(dataLength >= 2)
			this.radius = Integer.valueOf(info[1]);
		else
			this.radius = 0;
		if(dataLength >= 3)
			this.latitude = Double.valueOf(info[2]);
		else
			this.latitude = 0;
		if(dataLength >= 4)
			this.longitude = Double.valueOf(info[3]);
		else
			this.longitude = 0;
		
		u.log("Name:"+name);
		u.log("Radius:"+radius);
		u.log("Lat:"+latitude);
		u.log("Lon:"+longitude);
		
		if(dataLength < 4)
			this.setTimeOut(true);
		this.gameId = 0;
	}

	/**
	 * Uses the Clients obtained by GPS locations (double values) , No GeoPoints
	 **/
	public double distanceTo(Client p) {
		// http://www.movable-type.co.uk/scripts/latlong.html
		// Haversine formula

		double toRad = Math.PI / 180;
		int R = 6371; // Radius of Earth in km
		double dLat = (this.latitude - p.latitude) * toRad;
		double dLon = (this.longitude - p.longitude) * toRad;
		double lat1 = p.latitude * toRad;
		double lat2 = this.latitude * toRad;

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
				* Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		double distance = R * c;
		(new Utils()).log("Distance " + p.getName() + " - " + this.getName()
				+ " = " + distance);
		return distance;
	}

	/**
	 * Converts a Client object to a Player object.
	 * 
	 * @return the generated Player object
	 */
	Player toPlayer() {
		Player player = new Player();
		// id is set to player from Database
		player.setName(this.getName());
		player.setRadius(this.getRadius());
		player.setLatitude(this.getLatitude());
		player.setLongitude(this.getLongitude());
		player.setStatus(Player.WAITING);
		player.setProtocol(this.getProtocol());
		return player;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getTimeIn() {
		return timeIn;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public synchronized int getGameId() {
		return gameId;
	}

	public synchronized void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public synchronized Protocol getProtocol() {
		return protocol;
	}

	public synchronized void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public synchronized boolean isTimeOut() {
		return timeOut;
	}

	public synchronized void setTimeOut(boolean timeOut) {
		this.timeOut = timeOut;
	}

	public synchronized int getId() {
		return id;
	}

	public synchronized void setId(int id) {
		this.id = id;
	}

}
