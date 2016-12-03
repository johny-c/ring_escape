package edu.johny.ringescape.server;

import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Checks if the player escaped after the ESCAPE_TIME passes. Changes the
 * status of the victim either to FREE if he escaped or to IMPRISONED if
 * not. Adds reward points either to the victim if he escaped or to the
 * hitter if the victim got imprisoned. Also, starts a PrisonTask if the
 * victim got imprisoned.
 * @author Ioannis Chiotellis
 **/
public class HitTask extends TimerTask implements AppConstants{
	
	static double ESCAPE_DISTANCE_IN_KM = 0.02;
	// Hit Tasks use their own DB instance
	// because the SDB instance of the GameRunner class
	// might be closed by another Thread.
	// Ideally every Thread that interacts with the DB
	// must have his own DB instance
	// Otherwise they must ensure that
	// the DB instance they use is still open.
	private ServerDatabase hsdb = new ServerDatabase();
	private int taskId;
	private int victimId, hitterId;
	private double[] oldLocation = new double[2];
	private double[] newLocation;
	private List<TimerTask> tasks;	
	Utils u = new Utils();
	

	HitTask(HitUpdate hit, double[] victimLocation, int taskId, List<TimerTask> tasks) {
		this.victimId = hit.victimId;
		this.hitterId = hit.hitterId;
		this.oldLocation = victimLocation;
		this.taskId = taskId;
		this.tasks = tasks;
		u.log("HIT-TASK-" + taskId + " is CREATED !");
	}
	

	@Override
	public void run() {
		
		Thread.currentThread().setName("HIT-TASK-" + taskId);	
		u.log("CHECKS IF PLAYER HAS ESCAPED");
		hsdb.connect();

		int victimStatus = 0;
		try {
			victimStatus = hsdb.selectStatus(victimId);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		if (victimStatus == Player.QUITTED || victimStatus == Player.EXITED)
			return;

		newLocation = new double[2];
		try {
			newLocation = hsdb.getLocation(victimId);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		double distanceCovered = getDistance(oldLocation, newLocation);
		if (distanceCovered >= ESCAPE_DISTANCE_IN_KM) {
			u.log("Victim with id " + victimId + " escaped !");
			try {
				hsdb.updateStatus(victimId, Player.FREE);
				hsdb.updateScore(victimId, ESCAPE_REWARD_POINTS);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			u.log("Victim with id " + victimId + " gets imprisoned !");
			try {
				hsdb.updateStatus(victimId, Player.IMPRISONED);
				hsdb.updateScore(hitterId, ATTACK_REWARD_POINTS);
				PrisonTask prisonTask = new PrisonTask(victimId);
				Timer timer = new Timer();
				timer.schedule(prisonTask, PRISON_TIME);
				tasks.add(prisonTask);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		hsdb.close();
	}
	

	/** Returns the distance in km between two locations **/
	public double getDistance(double[] location1, double[] location2) {
		// http://www.movable-type.co.uk/scripts/latlong.html
		// Haversine formula
		double toRad = Math.PI / 180;
		int R = 6371; // Radius of Earth in km
		double dLat = (location1[0] - location2[0]) * toRad;
		double dLon = (location1[1] - location2[1]) * toRad;
		double lat1 = location1[0] * toRad;
		double lat2 = location2[0] * toRad;

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
				  * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		double distance = R * c;
		u.log("Distance from initial location = " + distance + " km");
		return distance;
	}

}
