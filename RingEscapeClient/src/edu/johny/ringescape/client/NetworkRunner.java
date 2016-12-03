package edu.johny.ringescape.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/** 
 *  Network communication Runnable that runs in a separate Thread 
 *  @author Ioannis Chiotellis
**/
public class NetworkRunner implements Runnable, AppConstants {

	//private final String LOG_TAG = this.getClass().getSimpleName();
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private boolean connected = false;
	private ClientProtocol clientProtocol;
	private String SERVER_IP;
	private int SERVER_PORT;
	private Context mContext;
	private Player mPlayer;
	private SharedPreferences prefs;

	NetworkRunner(Context context, Player player) {
		mContext = context;
		mPlayer = player;
	}

	public void run() {

		// Connect to the server
		//Log.d(LOG_TAG, "C: Connecting...");
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SERVER_IP = prefs.getString("server_ip", DEFAULT_SERVER_IP);
		SERVER_PORT = Integer.valueOf(prefs.getString("server_port",
				DEFAULT_SERVER_PORT + ""));
try {
	//Log.d(LOG_TAG, "Trying to connect to " + SERVER_IP);
	socket = new Socket(SERVER_IP, SERVER_PORT);
	//socket.setSoTimeout(SOCKET_TIMEOUT);
	writer = new PrintWriter(socket.getOutputStream(), true);
	reader = new BufferedReader(new InputStreamReader(
			socket.getInputStream()));
	connected = true;
} catch (ConnectException e) {
	informUser(SERVER_DOWN);
} catch (UnknownHostException e) {
	informUser(UNKNOWN_SERVER);
	e.printStackTrace();
	//Log.e(LOG_TAG, "Unknown server IP address!");
} catch (IOException e) {
	informUser(SOCKET_EXCEPTION);
	e.printStackTrace();
	//Log.e(LOG_TAG, "IO Socket Exception!");
}

if (connected) {
	clientProtocol = new ClientProtocol(mContext, mPlayer);
	writer.println(INITIAL + ""+ mPlayer.initialData());
String inputLine, outputLine = null;

try {
	while ((inputLine = reader.readLine()) != null) {
		//Log.d(LOG_TAG, "Socket reader reads new line");
		try {
			outputLine = clientProtocol.processInput(inputLine);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		writer.println(outputLine); // respond to server

		if (outputLine.startsWith(EXIT + "")) {
			break; // exit while reading loop
		}
	}
} catch (IOException e) {
	e.printStackTrace();
	//Log.e(LOG_TAG, "Socket IO Exception while reading/writing!");
}

		} // End of if connected

		try {
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Intent i = new Intent(mContext, LocationService.class);
			mContext.stopService(i);
			//Log.d(LOG_TAG, "Stop Service (Self) and mlp");
		}
		//Log.d(LOG_TAG, "Exiting run method...");
	} // End of run method

	private void informUser(int finalAnswer) {
		Intent i = new Intent(iANSWER_UPDATE);
		i.putExtra("state", FINAL_ANSWER);
		i.putExtra("answer", finalAnswer);
		mContext.sendBroadcast(i);
	}

}
