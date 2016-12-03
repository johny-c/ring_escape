package edu.johny.ringescape.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * GameOrganizer follows a policy of creating maximum number of games.
 * This means that even if only two players are eligible to play in the
 * same game, the game will be played.
 * @author Ioannis Chiotellis
 */
class GameOrganizer implements Observer, AppConstants {

	private ServerDatabase sdb = new ServerDatabase();
	private List<Game> pendingGames = Collections.synchronizedList(new ArrayList<Game>());
	private List<Client> pendingClients = Collections.synchronizedList(new ArrayList<Client>());
	Utils u = new Utils();

	GameOrganizer() {
		sdb.connect();
	}

	/**
	 * Callback method that gets called when a new client is inserted in the
	 * ClientsQueue. The new client will either get into a pending game, or into
	 * the pending players list.
	 */
	@Override
	public void update(Observable _queue, Object _client) {

		ClientsQueue queue = (ClientsQueue) _queue;
		Client newClient = (Client) _client;

		u.log("GO observes new client: " + newClient.getName() + " "
				+ newClient.getRadius());
		boolean inGame = false;

		try {
			inGame = searchInPendingClients(newClient);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (!inGame)
			inGame = searchInPendingGames(newClient);

		if (!inGame) {
			pendingClients.add(newClient);
			u.log("GO adds client to pending Clients");
		}

		// does not throw exception if null is polled (empty queue)
		queue.remove(newClient);
		u.log("GO end of update");
	}

	/** Searches in the pendingClients List for clients in range **/
	private boolean searchInPendingClients(Client newClient)
			throws SQLException {
		boolean found = false;
		Client pendingClient = null;
		double distance;

		u.log("Searching in Pending Clients");
		synchronized (pendingClients) {
			Iterator<Client> i = pendingClients.iterator(); // Must be in
															// synchronized
															// block
			while (i.hasNext() && found == false) {
				pendingClient = i.next();

				if (pendingClient.isTimeOut()) {					
					u.log("time out, "+pendingClient.getName()+" removed from pendingClients");
					i.remove();
					continue;
				}

				distance = newClient.distanceTo(pendingClient);
				if (distance < newClient.getRadius()
						&& distance < pendingClient.getRadius()) {
					// an exei vale kai tous 2 se neo paixnidi
					long startTime = pendingClient.getTimeIn()
							+ MAX_SEARCHING_TIME - System.currentTimeMillis();
					Game game = new Game(startTime);
					u.log("GO CLIENTS MATCH ! ! ! : " + newClient.getName()
							+ ", " + pendingClient.getName()
							+ ", Game starts in " + startTime + " ms");

					Player p1 = pendingClient.toPlayer();
					game.add(p1);
					pendingClient.setId(p1.getId());
					pendingClient.setGameId(p1.getGameId());
					i.remove(); // vgale ton sympaikti apo tous pendingClients

					Player p2 = newClient.toPlayer();
					game.add(p2);
					newClient.setId(p2.getId());
					newClient.setGameId(p2.getGameId());
					pendingGames.add(game); // prosthese to paixnidi sta
											// paixnidia pou perimenoun
					found = true;
				}
			}
		}
		u.log("End searching in Pending Clients");
		return found;
	}

	public static void main(String[] args) {

		String d = "\nJohny\nAnd\nBen\n\n\nend";
		d = d.replaceAll("\n", "");
		System.out.println(d);
	}

	/** Searches in the pendingGames List for games with all players in range **/
	private boolean searchInPendingGames(Client newClient) {
		boolean found = false;
		Game game = null;
		u.log("PG Searching in pending games");

		synchronized (pendingGames) {
			Iterator<Game> i = pendingGames.iterator(); // Must be in
														// synchronized block

			while (i.hasNext() && found == false) {
				game = i.next();

				if (game.hasStarted()) {
					u.log("PG I see Game has started or has already 10 players");
					i.remove();
					continue; // go to the next game
				}

				boolean gameMatch = true;
				double distance;
				Iterator<Player> ip = game.iterator();
				while (ip.hasNext() && gameMatch == true) {
					Player pp = ip.next();

					distance = newClient.distanceTo(pp);
					if (distance < newClient.getRadius()
							&& distance < pp.getRadius()) { // an einai
															// katallilos
						u.log("Match with player " + pp.getName());
					} else {
						u.log("Doesn't match with player" + pp.getName());
						gameMatch = false; // alliws psakse sto epomeno paixnidi
					}

				}

				if (gameMatch) { // an oloi oi paiktes tou paixnidiou einai
									// katalliloi
					u.log("PG GAME MATCH ! ! ! : " + game.getGame_id());
					Player p = newClient.toPlayer();
					game.add(p); // vale ton paikti sto paixnidi
					newClient.setId(p.getId());
					newClient.setGameId(p.getGameId());
					if (game.size() == MAX_PLAYERS_PER_GAME) {
						i.remove();
					}
					found = true; // min psakseis se allo paixnidi
				}
			}
		}
		u.log("End searching in Pending Games");
		return found;
	}

}
