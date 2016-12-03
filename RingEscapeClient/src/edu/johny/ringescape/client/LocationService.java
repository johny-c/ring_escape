package edu.johny.ringescape.client;

import java.io.IOException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

public class LocationService extends Service implements LocationListener,
		AppConstants {

/** 
  *  Service that receives location updates from Location Provider
  *  and starts the network communication Thread 
  *  @author Ioannis Chiotellis
 **/
	//private final String LOG_TAG = this.getClass().getSimpleName();
	private Context mContext;
	private Player player = new Player();
	private Thread thread;
	private String provider;
	private LocationManager locationManager;
	private MockLocationProviderTask mlp;
	private boolean firstFix = true, gameOn = false, virtual = false;
	private boolean useLastKnownLocation = false;
	private BroadcastReceiver qr;
	private SharedPreferences prefs;

	// Binder given to clients (activities) of the service
	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		LocationService getService() {
			return LocationService.this;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mContext = LocationService.this;

		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		player.setName(prefs.getString("name", "You"));
		player.setRadius(Integer.valueOf(prefs.getString("radius", "1")));
		virtual = prefs.getBoolean("virtual", false);
		useLastKnownLocation = prefs.getBoolean("useLastKnownLocation", false);
		//Log.d(LOG_TAG, "Virtual = " + virtual);
		setupLocation();

		IntentFilter filter = new IntentFilter(iEARLY_QUIT);
		qr = new QuitBroadcastReceiver();
		registerReceiver(qr, filter);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (virtual)
			mlp.cancel(true);
		if(locationManager != null)
		locationManager.removeUpdates(this);
		if (thread != null)
			if (thread.isAlive())
				thread.interrupt();
		if (qr != null)
			unregisterReceiver(qr);
		super.onDestroy();
	}

	/** Sets up the LocationListener **/
	private void setupLocation() {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		provider = LocationManager.GPS_PROVIDER;	
		if (virtual)
			setupMock();
		else if(useLastKnownLocation){
			Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
			if(lastKnownLocation != null)
				startCommunication(lastKnownLocation);
		}
		locationManager.requestLocationUpdates(provider, 0, 0, this);		
	}


	// This method sets up the Mock Location Provider
	private void setupMock() {
		locationManager.addTestProvider(provider, "requiresNetwork" == "",
				"requiresSatellite" == "", "requiresCell" == "",
				"hasMonetaryCost" == "", "supportsAltitude" == "",
				"supportsSpeed" == "", "supportsBearing" == "",
				Criteria.NO_REQUIREMENT, Criteria.ACCURACY_FINE);
		locationManager.setTestProviderEnabled(provider, true);

		String mockDataFileName = "GPSTrack.csv";
		try {
			mlp = new MockLocationProviderTask(locationManager, provider,
					mockDataFileName, mContext);
		} catch (IOException e) {
			String str = "Problem with mock location data file !\n";
			Toast.makeText(mContext, str + e.toString(), Toast.LENGTH_LONG)
					.show();
			e.printStackTrace();
		}

		mlp.execute();
	}

	
	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {

			if (gameOn) {
				// Do NOT store in local DB
				Intent i = new Intent(iLOCATION_UPDATE);
				i.putExtra("location", location.getLatitude() + FIELD_DELIM
						+ location.getLongitude());
				sendBroadcast(i); // Send to server through NetworkRunner
			} else if (firstFix) {
				startCommunication(location);
			}
		} else{
			//Log.d(LOG_TAG, "Loc Fix = null");
		}
	}
	
	
	// When first location fix is available
	// send request to the Server
	private void startCommunication(Location location) {
		player.setLatitude(location.getLatitude());
		player.setLongitude(location.getLongitude());
		Intent i = new Intent(iANSWER_UPDATE);
		i.putExtra("state", INITIAL);
		sendBroadcast(i);
		thread = new Thread(new NetworkRunner(mContext, player));
		thread.start(); // starts communication with server
		firstFix = false;	
	}
	

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		switch(status){
		case LocationProvider.AVAILABLE:
			toast(provider+" is available now !");
			break;
			
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			toast(provider+" is temporarily unavailable. Please wait!");
			break;
			
		case LocationProvider.OUT_OF_SERVICE:
			toast(provider+" is out of service ! Sorry...");
			break;
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		toast(provider+" is enabled !");
	}

	@Override
	public void onProviderDisabled(String provider) {
		toast("Please enable "+provider+" provider !");
	}

	@Override
	public IBinder onBind(Intent intent) {
		// RingEscape MapActivity binds
		gameOn = true;
		//Log.d(LOG_TAG, "Someone RingEscape binded");
		return mBinder;
	}

	/** Receives quit signal from NewGame or RingEscape **/
	public class QuitBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.d(LOG_TAG, "Got quit signal");
			if (firstFix) {
				//Log.d(LOG_TAG, "Stopping LocationListener and self");
				if (virtual)
					mlp.cancel(true);
				stopSelf();
			}
		}
	}

	/** Utility method to quickly toast a string **/
	protected void toast(String string) {
		//Log.d("LocationService", string);
		Toast t = new Toast(mContext);
		t.setDuration(Toast.LENGTH_LONG);
		t.setText(string);
		t.setGravity(Gravity.TOP, 0, 0);
		t.show();
	}

}
