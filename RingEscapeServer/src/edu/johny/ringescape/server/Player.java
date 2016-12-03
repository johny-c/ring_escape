package edu.johny.ringescape.server;

/** 
 * Class that contains information about a player 
 * @author Ioannis Chiotellis
 **/
public class Player extends Client {

	private int status;
	private GameRunner gameThread;
	private int score;
	private int highScore;

	// possible status values
	final static int EXITED = 0;
	final static int WAITING = 1;
	final static int FREE = 2;
	final static int THREATENED = 3;
	final static int IMPRISONED = 4;
	final static int QUITTED = 5;

	Player() {
		super();
		this.setGameId(0);
		this.setGameThread(null);
	}

	Player(String name) {
		this();
		super.setName(name);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public GameRunner getGameThread() {
		return gameThread;
	}

	public void setGameThread(GameRunner gameThread) {
		this.gameThread = gameThread;
	}

}
