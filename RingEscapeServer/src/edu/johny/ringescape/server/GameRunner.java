package edu.johny.ringescape.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/** 
 * Runnable class that runs during a game 
 * and handles players statuses changes 
 * @author Ioannis Chiotellis
 **/
public class GameRunner implements Runnable, AppConstants {

	private ArrayList<Player> players;
	private final int game_id;
	private int gameBarrierCounter = 0, hitTaskCounter = 0, N;
	private CyclicBarrier initBarrier, startBarrier, gameBarrier;
	private Runnable gameBarrierRunnable;
	private String gameUpdate;
	private Timer endGameTimer;
	private EndGameTask endGameTask = new EndGameTask();
	private ServerDatabase sdb = new ServerDatabase();
	private boolean endOfGame = false;
	private List<Protocol> clients;
	private List<HitUpdate> empty = new ArrayList<HitUpdate>();
	private List<TimerTask> tasks = new ArrayList<TimerTask>();
	Utils u = new Utils();

	GameRunner(Game game, List<Protocol> runners) {
		players = game;
		game_id = game.getGame_id();
		clients = runners;
		N = clients.size();
		sdb.connect();
	}

	@Override
	public void run() {

		/** Set the initialization barrier for the players **/
		u.log("RUNS");

		initBarrier = new CyclicBarrier(N + 1, new Runnable() {
			public void run() {
				u.log("INITBARRIER RUNNABLE STARTS ... clients size = "
						+ clients.size() + " , N = " + N);
				checkEarlyQuits();
				u.log("INITBARRIER RUNNABLE STARTS ... clients size = "
						+ clients.size() + " , N = " + N);
				try {
					sdb.initStatuses(game_id);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				gameUpdate = initGameData();
				u.log("INIT BARRIER RUNNABLE RUNS");

				Iterator<Protocol> i0 = clients.iterator();
				while (i0.hasNext()) {
					i0.next().setGameUpdate(gameUpdate);
				}

			}
		});

		Iterator<Protocol> i1 = clients.iterator();
		while (i1.hasNext())
			i1.next().setInitBarrier(initBarrier);

		try {
			initBarrier.await();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		} catch (BrokenBarrierException e2) {
			e2.printStackTrace();
		}

		/** Set the start barrier for the players **/
		startBarrier = new CyclicBarrier(N + 1, new Runnable() {
			public void run() {
				u.log("STARTBARRIER RUNNABLE STARTS ... clients size = "
						+ clients.size() + " , N = " + N);
				checkEarlyQuits();
				u.log("STARTBARRIER RUNNABLE ENDS ... clients size = "
						+ clients.size() + " , N = " + N);
			}
		});
		Iterator<Protocol> i2 = clients.iterator();
		while (i2.hasNext()) {
			i2.next().setStartBarrier(startBarrier);
		}

		try {
			startBarrier.await();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		} catch (BrokenBarrierException e2) {
			e2.printStackTrace();
		}
		endGameTimer = new Timer();
		endGameTimer.schedule(endGameTask, GAME_DURATION);
		u.log("- - - START BARRIER CROSSED, SETTING END GAME TASK - - - ");

		/** Define the Game Barrier Runnable **/
		gameBarrierRunnable = new Runnable() {
			public void run() {
				u.log("GAMEBARRIER RUNNABLE STARTS ... clients size = "
						+ clients.size() + " , N = " + N);
				long start = System.nanoTime();
				// Create a list with all hits of this update
				List<HitUpdate> allHits = new ArrayList<HitUpdate>();

				Iterator<Protocol> i3 = clients.iterator();
				while (i3.hasNext()) {
					Protocol p = i3.next();
					List<HitUpdate> list = p.getHitUpdates();
					p.setHitUpdates(empty);
					if (list != null) {
						for (HitUpdate h : list)
							u.log("hits of " + h.hitterId + ": " + h.victimId
									+ " at " + h.timeStamp);
						allHits.addAll(list);
					} else
						u.log("This hit list is null...");
				}

			if (!allHits.isEmpty()) {

				Collections.sort(allHits); // sort the list by the time
											// stamp of the hits
				u.log("*************ALL hits size = " + allHits.size());
				// change the status of those who are hit
				for (HitUpdate hit : allHits) {
					u.log("Hit: " + hit.hitterId + " hit " + hit.victimId
							+ " at " + hit.timeStamp
							+ " ns after game start");
				try {

					if (sdb.selectStatus(hit.hitterId) == Player.FREE)
						if (sdb.selectStatus(hit.victimId) == Player.FREE){
							sdb.storeThreat(hit.hitterId, hit.victimId);
							double[] location = new double[2];
							location = sdb.getLocation(hit.victimId);
							HitTask hitTask = new HitTask(hit, 
												location, 
												(++hitTaskCounter),
												tasks);
							Timer hitTimer = new Timer();
							hitTimer.schedule(hitTask, ESCAPE_TIME);
							tasks.add(hitTask);
						}

				} catch (SQLException e) {
					e.printStackTrace();
				}
			  }
			} else
				u.log("**************ALL HITS IS EMPTY ! ! !");

				// set the game update for the client threads to send back
				try {
					gameUpdate = sdb.getGameUpdate(game_id);
					Iterator<Protocol> i4 = clients.iterator();
					while (i4.hasNext()) {
						Protocol p = i4.next();
						p.setGameUpdate(gameUpdate);
						p.setGameBarrier(null);
					}

				} catch (SQLException e) {
					e.printStackTrace();
				}

				checkEarlyQuits();
				long load = System.nanoTime() - start;
				u.log("GAMEBARRIER RUNNABLE ENDS after " + load + " ns");
			}
		};

		while (!isEndOfGame()) {

			gameBarrier = new CyclicBarrier(N + 1, gameBarrierRunnable);
			Iterator<Protocol> i5 = clients.iterator();
			while (i5.hasNext())
				i5.next().setGameBarrier(gameBarrier);

			u.log("SETS THE GAMEBARRIER " + (++gameBarrierCounter));

			try {
				gameBarrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
		}

		u.log("- - - - - - HAS SEEN END OF GAME TRUE IS TRUE - - - - - -\n");

		Iterator<Protocol> i6 = clients.iterator();
		u.log("SEEING clients size = " + clients.size() + " , N = " + N);
		while (i6.hasNext()) {
			i6.next().setEndOfGame(true);
		}

		// Cancels hitTasks and prisonTasks that are scheduled
		// to run in the future but have not run yet.
		// If a task is currently running it will complete.
		for (TimerTask tt : tasks) {
			tt.cancel();
			u.log("Cancelling Task " + tt.toString());
		}
		
		
		// delete locations from DB
		try {
			sdb.deleteLocations(game_id);
		} catch (SQLException e) {
			e.printStackTrace();
		}		

		sdb.close();
	}

	/** Checks if a player has exited the game early **/
	private void checkEarlyQuits() {
		Iterator<Protocol> i = clients.iterator();
		while (i.hasNext()) {
			Protocol p = i.next();
			if (p.getEarlyQuit()) {
				u.log("***************Seeing Early Quit - Removing client ");
				i.remove();
				N--;
			}
		}
	}

	/** Returns initialization data for the game **/
	String initGameData() {

		String result = "";
		// other players
		for (Player p : players) {
			result += p.getId() + FIELD_DELIM;
			result += p.getName() + FIELD_DELIM;
			result += p.getLatitude() + FIELD_DELIM;
			result += p.getLongitude() + REC_DELIM;
		}
		return result;
	}

	
	/** Sets the end Of Game flag to true after the GAME_DURATION passes **/
	class EndGameTask extends TimerTask {

		@Override
		public void run() {
			Thread.currentThread().setName("END-GAME-TASK");
			setEndOfGame(true);
			u.log("- - - - - - - - - END OF GAME IS TRUE - - - - - - - - -\n");
		}
	}


	/** Sets the endOfGame flag (to true) **/
	private synchronized void setEndOfGame(boolean value) {
		endOfGame = value;
	}

	/** Gets the endOfGame flag **/
	private synchronized boolean isEndOfGame() {
		return endOfGame;
	}

}
