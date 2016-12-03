package edu.johny.ringescape.server;

import java.sql.SQLException;
import java.util.TimerTask;

/**
 * Releases a player who got imprisoned after the PRISON_TIME passes. 
 * Sets his status to FREE again.
 * @author Ioannis Chiotellis
 **/
public class PrisonTask extends TimerTask {

	private ServerDatabase psdb = new ServerDatabase(); // one db connection per thread
	private int playerId;
	Utils u = new Utils();

	PrisonTask(int playerId) {
		this.playerId = playerId;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("A PRISON-TASK");
		u.log("releasing player with id " + playerId);
		psdb.connect();

		int victimStatus = 0;
		try {
			victimStatus = psdb.selectStatus(playerId);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		if (victimStatus == Player.QUITTED || victimStatus == Player.EXITED){
			psdb.close();
			return;
		}
			

		try {
			psdb.updateStatus(playerId, Player.FREE);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		psdb.close();
	}
}
