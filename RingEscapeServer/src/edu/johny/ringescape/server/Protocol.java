package edu.johny.ringescape.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Class that handles all incoming/outgoing messages 
 * between the Server and the Client
 * @author Ioannis Chiotellis
 **/
class Protocol implements AppConstants {

	private ClientsQueue clientsQueue;
	private int puc = 0;
	private int searchingTime = 0;
	private int playersFound = 0;
	private Client client;
	private ServerDatabase resdb;
	private boolean endOfGame;
	private String gameUpdate;
	private List<HitUpdate> hitUpdates;
	private CyclicBarrier initBarrier, startBarrier, gameBarrier;
	private boolean earlyQuit = false;
	private String lastMsgOut;
	Utils u = new Utils();

	public Protocol(ClientsQueue cq) {
		this.clientsQueue = cq;
		this.resdb = new ServerDatabase();
		resdb.connect();
	}


/**
 * Processes the incoming messages and returns appropriate outgoing
 * messages.
 * 
 * @param msgIn
 * @return msgOut
 * @throws SQLException
 */
public String processInput(String msgIn) throws SQLException {

	if (msgIn == null)
		return lastMsgOut;

	int msgType;
	try {
		msgType = Integer.valueOf(msgIn.substring(0, 1));
	} catch (NumberFormatException e) {
		msgType = UNKNOWN_MESSAGE_TYPE;
		e.printStackTrace();
	}

	String msgContent = msgIn.substring(1);
	String msgOut = null;

	switch (msgType) {

	case INITIAL:
		client = new Client(this, msgContent);
		String prev = Thread.currentThread().getName();
		Thread.currentThread().setName("SERVER-" + client.getName());
		u.log("was named " + prev);
		u.log("received INITIAL");
		u.log("TimeIn = " + client.getTimeIn());
		boolean space = clientsQueue.add(client);

		if (space == false) {
			u.log("sends SERVER_TOO_BUSY (EXIT): " + SERVER_TOO_BUSY);
			resdb.close();
			msgOut = SERVER_TOO_BUSY + "";
		} else {
			sleep(SLEEPING_TIME);
			playersFound = getPlayersFound();
			msgOut = ANSWER_UPDATE + "" + playersFound;
		}
		break;

	case ANSWER_RECEIVED:
		u.log("received ANSWER_RECEIVED");
		if (searchingTime >= MAX_SEARCHING_TIME) {
			// set player's time out so he cannot
			// be picked by the GameOrganizer anymore
			client.setTimeOut(true);
			if (playersFound == 0) {
				resdb.close();
				msgOut = NO_PLAYERS_FOUND + "";
			} else
				msgOut = FINAL_ANSWER + "" + playersFound;
			u.log("sends FINAL_ANSWER: " + playersFound);
		} else {
			sleep(SLEEPING_TIME);
			playersFound = getPlayersFound();
			msgOut = ANSWER_UPDATE + "" + playersFound;
			u.log("sends ANSWER_UPDATE: " + playersFound);
		}
		break;

	case FINAL_RECEIVED:
		waitForInitBarrier();
		msgOut = INIT_GAME + "" + client.getId() + REC_DELIM
				+ getGameUpdate();
		u.log("sends INIT_GAME: " + msgOut);
		break;

	case READY:
		waitForStartBarrier();
		msgOut = START_GAME + "";
		u.log("sends START_GAME SIGNAL");
		break;

	case PLAYER_UPDATE:
		u.log("received PLAYER UPDATE " + (++puc) + ":" + msgContent);

		if (!isEndOfGame()) {
			handlePlayerUpdate(msgContent);
			waitForGameBarrier();
			// wait until all players of this GameRunner
			// have reached this point
			msgOut = GAME_UPDATE + "" + getGameUpdate();
		} else {
			u.log("- - - HAS SEEN END OF GAME TRUE IS TRUE" + " - - -\n");
			u.log("sends End Of Game !");
			resdb.close();
			msgOut = END_OF_GAME + "";
		}
		break;

	case EXIT:
		u.log("received EXIT: " + msgContent);
		if (msgIn.startsWith(EARLY_QUIT + "")) {
			client.setTimeOut(true);
			setEarlyQuit(true);
			int dbId = client.getId();
			if (dbId != 0)
				resdb.updateStatus(dbId, Player.QUITTED);
			u.log("Setting early quit true\n\n\n\n\n");
			int lastMsgType = Integer.valueOf(msgIn.substring(2));
			handleEarlyQuit(lastMsgType);
			msgOut = EXIT + "";
		} else if(msgContent.equals(UNKNOWN_MESSAGE_TYPE+"")){
			client.setTimeOut(true);
			int dbId = client.getId();
			if (dbId != 0)
				resdb.updateStatus(dbId, Player.QUITTED);
			u.log("Setting unknown message type true\n\n\n\n\n");
			//int lastMsgType = Integer.valueOf(msgIn.substring(2));
			//handleEarlyQuit(lastMsgType);
			msgOut = EXIT + "";
		} else if(msgIn.startsWith(SET_VAR + "")){
			HitTask.ESCAPE_DISTANCE_IN_KM = Double.valueOf(msgIn.substring(2));
			msgOut = SET_VAR +"1";				
			u.log("HitTask.ESCAPE_DISTANCE_IN_KM = "+HitTask.ESCAPE_DISTANCE_IN_KM);
		}
		
		resdb.close();
		break;

	case UNKNOWN_MESSAGE_TYPE:

	default:
		msgOut = EXIT+""+UNKNOWN_MESSAGE_TYPE;
		break;
	}

	lastMsgOut = msgOut;
	return msgOut;
}


	/**
	 * If the client/player exited early, resources must be handled and released
	 **/
	private void handleEarlyQuit(int msgType) {

		switch (msgType) {

		case ANSWER_UPDATE:
			clientsQueue.remove(client);
			u.log("***********************EARLY QUIT - REMOVING CLIENT FROM QUEUE");
			if (client.getGameId() != 0)
				waitForInitBarrier();
			break;

		case FINAL_ANSWER:
			u.log("***********************EARLY QUIT - WAITING FOR INITBARRIER");
			waitForInitBarrier();
			break;

		case INIT_GAME:
			u.log("***********************EARLY QUIT - WAITING FOR STARTBARRIER");
			waitForStartBarrier();
			break;

		case START_GAME:
			u.log("***********************EARLY QUIT - WAITING FOR GAMEBARRIER");
			waitForGameBarrier();
			break;

		case GAME_UPDATE:
			if (!isEndOfGame()) {
				u.log("***********************EARLY QUIT - WAITING FOR GAMEBARRIER - NOT END OF GAME");
				waitForGameBarrier();
			}

			break;
		default:
			break;
		}

	}

	/**
	 * Checks how many players are in the same game as the player.
	 * 
	 * @return 0 if player is not inserted in a game or players found
	 */
	private int getPlayersFound() {

		searchingTime += SLEEPING_TIME;
		int g = client.getGameId();
		if (g == 0)
			return 0;
		else
			try {
				return (resdb.getPlayersInGame(g) - 1);
			} catch (SQLException e) {
				e.printStackTrace();
				return 0;
			}
	}

	/** Utility method to sleep for time milliseconds **/
	private void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Waits until the initBarrier of the game is set by the GameRunner, and all
	 * players have reached this point.
	 */
	private void waitForInitBarrier() {
		while (getInitBarrier() == null) {
			u.log("received READY, waiting for INIT-Barrier ");
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		u.log("got the INIT-Barrier, now awaiting");
		try {
			initBarrier.await(); // synchronize players for the game
									// initialization
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Waits until the startBarrier of the game is set by the GameRunner, and
	 * all players have reached this point.
	 */
	private void waitForStartBarrier() {
		while (getStartBarrier() == null) {
			u.log("received READY, waiting for START-Barrier ");
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		u.log("got the START-Barrier, now awaiting");
		try {
			startBarrier.await(); // synchronize players for the game start
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Waits until the gameBarrier of the game is set by the GameRunner, and all
	 * players have reached this point.
	 */
	private void waitForGameBarrier() {
		while (getGameBarrier() == null) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			gameBarrier.await(); // synchronize players for every update
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the player update received from the Client Side. Extracts
	 * location update and stores it in the database. Extracts hit updates list
	 * if there is one and makes it available to the GameRunner.
	 */
	private void handlePlayerUpdate(String msgIn) throws SQLException {
		String updates[] = msgIn.split(UPD_DELIM);

		if (!updates[0].isEmpty())
			resdb.updateLocation(client.getId(), updates[0]); // location in DB
		if (updates.length > 1) {
			List<HitUpdate> list = buildHitUpdates(updates[1]);
			setHitUpdates(list);
		}
	}

	/**
	 * Builds the hitUpdates list if there is one received from the Client Side.
	 * 
	 * @return hitUpdates
	 */
	private List<HitUpdate> buildHitUpdates(String hitUpdates) {
		List<HitUpdate> list = new ArrayList<HitUpdate>();

		String[] hits = hitUpdates.split(REC_DELIM);
		for (String hit : hits) {
			u.log("***************HIT= " + hit);
			String[] hitInfo = hit.split(FIELD_DELIM);
			HitUpdate hitUpdate = new HitUpdate(client.getId(), hitInfo[0],
					hitInfo[1]);
			list.add(hitUpdate);
		}

		return list;
	}

	/** Sets the initBarrier and notifies the thread to stop waiting **/
	synchronized void setInitBarrier(CyclicBarrier initBarrier) {
		this.initBarrier = initBarrier;
		this.notify();
	}

	/**
	 * Gets the initBarrier
	 * 
	 * @return initBarrier
	 **/
	private synchronized CyclicBarrier getInitBarrier() {
		return initBarrier;
	}

	/** Sets the startBarrier and notifies the thread to stop waiting **/
	synchronized void setStartBarrier(CyclicBarrier startBarrier) {
		this.startBarrier = startBarrier;
		this.notify();
	}

	/**
	 * Gets the startBarrier
	 * 
	 * @return startBarrier
	 **/
	private synchronized CyclicBarrier getStartBarrier() {
		return startBarrier;
	}

	/** Sets the gameBarrier and notifies the thread to stop waiting **/
	synchronized void setGameBarrier(CyclicBarrier gameBarrier) {
		this.gameBarrier = gameBarrier;
		this.notify();
	}

	/**
	 * Gets the gameBarrier
	 * 
	 * @return gameBarrier
	 **/
	private synchronized CyclicBarrier getGameBarrier() {
		return gameBarrier;
	}

	/** Sets the gameUpdate to be sent back to the Client Side **/
	synchronized void setGameUpdate(String gameUpdate) {
		this.gameUpdate = gameUpdate;
	}

	/**
	 * Gets the gameUpdate set by the GameRunner
	 * 
	 * @return gameUpdate
	 **/
	private synchronized String getGameUpdate() {
		return gameUpdate;
	}

	/** Sets the hitUpdates list **/
	synchronized void setHitUpdates(List<HitUpdate> hu) {
		this.hitUpdates = hu;
	}

	/**
	 * Gets the hitUpdates list
	 * 
	 * @return hitUpdates
	 **/
	synchronized List<HitUpdate> getHitUpdates() {
		return hitUpdates;
	}

	/** Sets the end of game flag **/
	synchronized void setEndOfGame(boolean endOfGame) {
		this.endOfGame = endOfGame;
	}

	/**
	 * Gets the end of game flag
	 * 
	 * @return endOfGame
	 **/
	private synchronized boolean isEndOfGame() {
		return endOfGame;
	}

	/** Sets the early quit flag **/
	private synchronized void setEarlyQuit(boolean b) {
		this.earlyQuit = b;
	}

	/**
	 * Gets the early quit flag
	 * 
	 * @return earlyQuit
	 **/
	synchronized boolean getEarlyQuit() {
		return earlyQuit;
	}

}
