package edu.johny.ringescape.client;

import android.location.Location;

/** 
 * Class that contains information about a player 
 * @author Ioannis Chiotellis
 **/
public class Player implements AppConstants {

	private int id;
	private String name;
	private int radius;
	private double latitude;
	private double longitude;
	private int score;
	private int highScore;
	private int status;
	private int hitter;
	private Location ringCenter;
	private double correctingFactor;

	// possible status values
	static final int EXITED = 0;
	static final int WAITING = 1;
	static final int FREE = 2;
	static final int THREATENED = 3;
	static final int IMPRISONED = 4;
	static final int QUITTED = 5;

	public Player() {
		this.name = "You";
		this.latitude = 0;
		this.longitude = 0;
		this.score = 0;
		this.highScore = 0;
		this.status = Player.FREE;
		this.hitter = 0;
	}

	public Player(String name) {
		this();
		this.name = name;
	}

	public String initialData() {
		String str = "";
		str += this.getName() + FIELD_DELIM;
		str += this.getRadius() + FIELD_DELIM;
		str += this.getLatitude() + FIELD_DELIM;
		str += this.getLongitude();
		return str;
	}

	/**
	 * Uses the Clients - obtained by GPS - 
	 * locations (double values), Not GeoPoints
	 **/
	public double distanceTo(Player p) {
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
		return distance;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getHighScore() {
		return highScore;
	}

	public void setHighScore(int highScore) {
		this.highScore = highScore;
	}

	public synchronized int getStatus() {
		return status;
	}

	public synchronized void setStatus(int status) {
		this.status = status;
	}

	public int getHitter() {
		return hitter;
	}

	public void setHitter(int hitter) {
		this.hitter = hitter;
	}

	public Location getRingCenter() {
		return ringCenter;
	}

	public void setRingCenter(Location ringCenter) {
		this.ringCenter = ringCenter;
	}

	public void setCorrectingFactor(double d) {
		this.correctingFactor = d;
	}

	public double getCorrectingFactor() {
		return correctingFactor;
	}

}
