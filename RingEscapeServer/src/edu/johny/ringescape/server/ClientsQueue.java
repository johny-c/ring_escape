package edu.johny.ringescape.server;

import java.util.Observable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Class that represents a Queue of requesting Clients 
 * (observable by the GameOrganizer)
 * @author Ioannis Chiotellis
 **/
public class ClientsQueue extends Observable {

	private BlockingQueue<Client> clients;
	Utils u = new Utils();

	ClientsQueue(final int max) {
		clients = new ArrayBlockingQueue<Client>(max, true);

	}

	boolean add(Client p) {
		boolean space = clients.offer(p);

		if (space) {
			u.log("QUEUE adds client: " + space);
			this.setChanged();
			u.log("QUEUE sets this changed");
			this.notifyObservers(p);
			u.log("QUEUE notifies Observers p");
		}
		return space;
	}

	Client poll() {
		return clients.poll();
	}

	boolean remove(Client p) {
		u.log("QUEUE removes client: ");
		return clients.remove(p);
	}

}
