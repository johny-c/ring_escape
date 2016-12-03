package edu.johny.ringescape.client;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Class that handles all incoming/outgoing messages 
 * between the Client and the Server
 * @author Ioannis Chiotellis
 **/
public class ClientProtocol implements AppConstants {

	//private final String LOG_TAG = this.getClass().getSimpleName();
	private Player player;
	private MyDBAdapter mdba;
	private Context mContext;
	private Intent intent;
	private BroadcastReceiver hr, lr, qr;
	private IntentFilter hitFilter, locFilter, quitFilter;
	private String locationUpdate = "";
	private List<String> hitUpdates;
	private boolean quitSignal = false;

	public ClientProtocol(Context context, Player p) {
		this.mContext = context;
		this.player = p;
		this.mdba = new MyDBAdapter(mContext);
		quitFilter = new IntentFilter(iEARLY_QUIT);
		qr = new QuitBroadcastReceiver();
		mContext.registerReceiver(qr, quitFilter);
	}

	/** Handles incoming message and returns an appropriate outgoing message **/
	public String processInput(String msgIn) throws SQLException {
		String theOutput = null;
		if (msgIn == null){
			releaseResources();
			return EXIT+"";
		}
			
		//Log.d(LOG_TAG, "msgIn= " + msgIn);
		int msgType;
		String msgContent = "";
		
		if(msgIn.length() > 0){
			try {
				msgType = Integer.valueOf(msgIn.substring(0, 1));
				msgContent = msgIn.substring(1);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				msgType = EXIT;
				msgContent = msgIn;
				msgIn = UNKNOWN_MESSAGE_TYPE + "";	
			}
		}
		else{
			msgType = EXIT;
			msgIn = UNKNOWN_MESSAGE_TYPE + "";
		}
			
		
		
		if (getEarlyQuit()) {
			if (msgType != EXIT) {
				releaseResources();
				theOutput = EARLY_QUIT + "" + msgType;
				//Log.e(LOG_TAG, "Seeing EARLY_QUIT");
				return theOutput;
			}
		} else
			//Log.d(LOG_TAG, "No EARLY_QUIT signal yet...");

		switch (msgType) {

		case ANSWER_UPDATE:
			//Log.d(LOG_TAG, "C, answer update received");
			intent = new Intent(iANSWER_UPDATE);
			intent.putExtra("state", ANSWER_UPDATE);
			intent.putExtra("answer", Integer.valueOf(msgContent));
			mContext.sendBroadcast(intent);
			theOutput = ANSWER_RECEIVED + "";
			break;

		case FINAL_ANSWER:
			//Log.d(LOG_TAG, "C, final answer received");
			intent = new Intent(iANSWER_UPDATE);
			intent.putExtra("state", FINAL_ANSWER);
			intent.putExtra("answer", Integer.valueOf(msgContent));
			mContext.sendBroadcast(intent);

			// Prepare resources for game
			mdba.open();
			hitUpdates = Collections.synchronizedList(new ArrayList<String>());
			hitFilter = new IntentFilter(iHIT_UPDATE);
			hr = new HitBroadcastReceiver();
			mContext.registerReceiver(hr, hitFilter);
			locFilter = new IntentFilter(iLOCATION_UPDATE);
			lr = new LocationBroadcastReceiver();
			mContext.registerReceiver(lr, locFilter);
			theOutput = FINAL_RECEIVED + "";
			break;

		case INIT_GAME:
			//Log.d(LOG_TAG, "C, Init game signal received " + msgIn);
			String[] records = msgContent.split(REC_DELIM); // Store initial
															// data
			player.setId(Integer.valueOf(records[0]));
			//Log.d(LOG_TAG, "PlayerID = " + player.getId());
			for (int i = 1; i < records.length; i++) {
				mdba.insertPlayer(records[i], player.getId());
			}
			intent = new Intent(iANSWER_UPDATE); // then load map
			intent.putExtra("state", INIT_GAME);
			intent.putExtra("id", player.getId());
			intent.putExtra("lat", player.getLatitude());
			intent.putExtra("lon", player.getLongitude());
			mContext.sendBroadcast(intent);
			try {
				Thread.sleep(MAP_LOADING_TIME); // Give some time for the map to
												// load
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			theOutput = READY + "";
			break;

		case START_GAME:
			//Log.d(LOG_TAG, "C, Start game signal received");
			intent = new Intent(iGAME_UPDATE);
			intent.putExtra("startSignal", true);
			mContext.sendBroadcast(intent);
			theOutput = PLAYER_UPDATE + getLocationUpdate();
			break;

		case GAME_UPDATE:
			//Log.d(LOG_TAG, "C, game update received");
			mdba.updateGame(msgContent); // update local DB
			intent = new Intent(iGAME_UPDATE); // inform RingEscape that a new
												// update arrived
			intent.putExtra("update", true);
			mContext.sendBroadcast(intent);

			try {
				Thread.sleep(RESTING_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			String hits = "";
			synchronized (hitUpdates) {
				Iterator<String> i = hitUpdates.iterator(); // Must be in
															// synchronized
															// block
				while (i.hasNext()) {
					hits += (i.next() + REC_DELIM);
					i.remove();
				}
			}

			theOutput = getLocationUpdate();
			if (hits.endsWith(REC_DELIM)) {
				hits = hits.substring(0, hits.length() - 1);
				theOutput += UPD_DELIM + hits;
			}

			theOutput = PLAYER_UPDATE + theOutput;
			break;

		case EXIT:
			//Log.d(LOG_TAG, "C, exit signal received " + msgIn);
			int code = Integer.valueOf(msgIn);
			if (code == END_OF_GAME) {
				intent = new Intent(iGAME_UPDATE);
				intent.putExtra("endSignal", true);
				mContext.sendBroadcast(intent);
				mdba.setHighScores();
				//Log.d(LOG_TAG, "END OF GAME received");
			} 
			else{
				intent = new Intent(iANSWER_UPDATE);
				intent.putExtra("state", FINAL_ANSWER);			
				intent.putExtra("answer", code);
				if (code == UNKNOWN_MESSAGE_TYPE)
					intent.putExtra("message", msgContent);
				mContext.sendBroadcast(intent);
				if (code == UNKNOWN_MESSAGE_TYPE){
					intent.setAction("edu.johny.ringescape.client"+iGAME_UPDATE);
					mContext.sendBroadcast(intent);
				}
				//Log.d(LOG_TAG, "EXIT CODE"+ msgIn+" received");
			}
			
			releaseResources();
			theOutput = EXIT + "Bye";
			break;

		default:
			theOutput = UNKNOWN_MESSAGE_TYPE + "";
			break;
		}

		return theOutput;
	}

	/** Releases bound resources before exiting **/
	private void releaseResources() {
		if (lr != null)
			mContext.unregisterReceiver(lr);
		if (hr != null)
			mContext.unregisterReceiver(hr);
		if (qr != null)
			mContext.unregisterReceiver(qr);

		mdba.clearTable();
		mdba.close();
	}

	/** Receives hit updates from PlayersOverlay.onTap() **/
	public class HitBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.d(LOG_TAG, "GOT A HIT UPDATE");
			hitUpdates.add(intent.getStringExtra("hit"));
		}
	}

	/** Receives location updates from RingEscape.onLocationChanged() **/
	public class LocationBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//String n = Thread.currentThread().getName();
			//Log.d(LOG_TAG, "Got Location update " + n);
			setLocationUpdate(intent.getStringExtra("location"));
		}
	}

	/** Receives quit signal from NewGame or RingEscape **/
	public class QuitBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//String n = Thread.currentThread().getName();
			//Log.d(LOG_TAG, "Got quit signal, i'm thread " + n);
			setEarlyQuit(true);
		}
	}

	/** Gets the most recent location update to send to the Server **/
	synchronized private String getLocationUpdate() {
		//String n = Thread.currentThread().getName();
		//Log.d(LOG_TAG, "Getting Location update " + n);
		return locationUpdate;
	}

	/** Sets the latest location update **/
	synchronized private void setLocationUpdate(String update) {
		//String n = Thread.currentThread().getName();
		//Log.d(LOG_TAG, "Setting Location update " + n);
		locationUpdate = update;
	}

	/** Gets an early quit signal due to user early exit **/
	synchronized private boolean getEarlyQuit() {
		return quitSignal;
	}

	/** Sets an early quit signal on user's early exit **/
	synchronized private void setEarlyQuit(boolean value) {
		quitSignal = value;
	}

}
