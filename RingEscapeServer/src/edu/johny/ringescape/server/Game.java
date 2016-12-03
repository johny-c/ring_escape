package edu.johny.ringescape.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/** 
 * Class that represents the list of players that consist a game 
 * @author Ioannis Chiotellis
 **/
class Game extends ArrayList<Player> implements AppConstants {

	private static final long serialVersionUID = 1L;
	private ServerDatabase sdb = new ServerDatabase();
	private int game_id;
	private boolean started = false;
	private Timer gTimer;
	private StarterTask starter;
	Utils u = new Utils();

	Game(long startTimeLimit) throws SQLException {
		sdb.connect();
		this.game_id = sdb.insertGame(0);
		this.starter = new StarterTask(this);
		this.gTimer = new Timer();
		if (startTimeLimit < 0)
			startTimeLimit = 0;
		gTimer.schedule(starter, startTimeLimit);
	}

	/** Starts the GameRunner for this game **/
	public void start() {
		this.setStarted(true);
		//
		List<Protocol> lpts = new ArrayList<Protocol>(this.size());
		for (int i = 0; i < this.size(); i++) {
			lpts.add(this.get(i).getProtocol());
		}

		//
		Protocol[] pts = new Protocol[this.size()];
		for (int i = 0; i < this.size(); i++) {
			pts[i] = this.get(i).getProtocol();
		}

		Thread t = new Thread(new GameRunner(this, lpts)); // pts
		t.setName("GAME-THREAD");
		t.start();

		u.log("GAME STARTS GAME RUNNER");
	}

	/**
	 * Timer task that calls the start method when the time limit of the player
	 * that came in earlier expires
	 **/
	class StarterTask extends TimerTask {

		Game game;

		StarterTask(Game game) {
			this.game = game;
		}

		@Override
		public void run() {
			Thread.currentThread().setName("GAME-STARTER-TASK");
			if (game.hasStarted()) {
				u.log("tried but game is already started! ");
			} else {
				game.start();
				u.log("starts game ");
			}
		}
	}

	@Override
	public boolean add(Player p) {
		p.setGameId(this.game_id);
		try {
			int player_id = sdb.insert(p);
			p.setId(player_id);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return super.add(p);
	}

	synchronized void setStarted(boolean started) {
		this.started = started;
	}

	synchronized boolean hasStarted() {
		return started;
	}
	
	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}

}
