package edu.johny.ringescape.client;

import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/** 
 * Activity that shows the map. Main Activity during the game. 
 * @author Ioannis Chiotellis
 **/
public class RingEscape extends MapActivity implements AppConstants {

	//private final String LOG_TAG = this.getClass().getSimpleName();
	private Context mContext;
	private Chronometer timer;
	private TextView tvCountdown;
	private RelativeLayout rl;
	private MapView mapView;
	private MapController mapController;
	private List<Overlay> mapOverlays;
	private PlayersOverlay playersOverlay;
	private Drawable drawable;
	private Builder endDialog, backDialog;
	private ContextThemeWrapper ctw;
	private Player player = new Player();
	private StartTask startTask;
	private MyDBAdapter mdba;
	private Cursor playersCursor;
	private UpdateBroadcastReceiver r;
	private boolean mBound = false;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName component, IBinder binder) {
			// We've bound to NetworkService,
			// cast the IBinder and get NetworkService instance
			//mService = ((LocalBinder) binder).getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName component) {
			Toast.makeText(mContext, component.getShortClassName()
					+ " just disconnected from NetworkService..",
					Toast.LENGTH_SHORT);
			mBound = false;
			finish();
		}
	};

	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_view);
		mContext = RingEscape.this;

		// Set map
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setFocusable(true);
		mapView.setKeepScreenOn(true);

		// Find the relative layout
		rl = (RelativeLayout) findViewById(R.id.rl);

		// Set the chronometer
		timer = (Chronometer) findViewById(R.id.tv_timer2);
		timer.setBackgroundColor(Color.DKGRAY);

		// Set the countdown textview
		tvCountdown = (TextView) findViewById(R.id.tv_countdown);

		// Open DB connection and get players Cursor
		mdba = new MyDBAdapter(mContext);
		mdba.open();
		playersCursor = mdba.getGame();

		// Get this player's id and location
		Intent starter = this.getIntent();
		player.setId(starter.getIntExtra("id", 0));
		player.setLatitude(starter.getDoubleExtra("lat", 0));
		player.setLongitude(starter.getDoubleExtra("lon", 0));

		// Set this player's location as map's center
		mapController = mapView.getController();

		//Log.d(LOG_TAG, "My playersCursor has " + playersCursor.getCount()
		//		+ " rows");
		// Drawable is needed but not used
		drawable = this.getResources().getDrawable(R.drawable.ic_launcher);

		// Set PlayersOverlay (locations and statuses)
		playersOverlay = new PlayersOverlay(player, playersCursor, drawable,
				this);
		mapOverlays = mapView.getOverlays();
		mapOverlays.add(playersOverlay);
	}

	/**
	 * Callback method that is called when window gets dimensions Appropriate to
	 * zoom in
	 **/
	public void onWindowFocusChanged(boolean hasFocus) {
		//Log.d(LOG_TAG, "Focus changed to: " + hasFocus);
		//int h = mapView.getHeight();
		//int w = mapView.getWidth();
		//Log.d(LOG_TAG, "MapView params: w = " + w + " , h = " + h);

		if (hasFocus) {
			double maxLat = 0, minLat = 0, maxLon = 0, minLon = 0;
			if (playersCursor.moveToFirst()) {

				maxLat = playersCursor.getDouble(1);
				minLat = playersCursor.getDouble(1);
				maxLon = playersCursor.getDouble(2);
				minLon = playersCursor.getDouble(2);

				while (playersCursor.moveToNext()) {
					if (playersCursor.getDouble(1) < minLat)
						minLat = playersCursor.getDouble(1);
					else if (playersCursor.getDouble(1) > maxLat)
						maxLat = playersCursor.getDouble(1);

					if (playersCursor.getDouble(2) < minLon)
						minLon = playersCursor.getDouble(2);
					else if (playersCursor.getDouble(2) > maxLon)
						maxLon = playersCursor.getDouble(2);
				}
			}

			int mapCenterLat = (int) ((minLat + maxLat) * 1E6 / 2);
			int mapCenterLon = (int) ((minLon + maxLon) * 1E6 / 2);
			int latSpanE6 = (int) ((maxLat - minLat) * 1E6);
			int lonSpanE6 = (int) ((maxLon - minLon) * 1E6);
			mapController.setCenter(new GeoPoint(mapCenterLat, mapCenterLon));
			mapController.zoomToSpan(latSpanE6, lonSpanE6);
			mapController.zoomOut();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent i = new Intent(mContext, LocationService.class);
		i.putExtra("caller", "RingEscape");
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);

		// Create and register the broadcast receiver for messages from service
		IntentFilter filter = new IntentFilter(iGAME_UPDATE);
		r = new UpdateBroadcastReceiver();
		registerReceiver(r, filter);

		// Create the dialog for end of game
		// using the Activity's context to show up correctly
		ctw = new ContextThemeWrapper(mContext,
				android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
		endDialog = new AlertDialog.Builder(ctw);
		endDialog.setTitle("End of Game");
		endDialog.setCancelable(false);
		endDialog.setNeutralButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent highScores = new Intent(RingEscape.this,
						HighScores.class);
				startActivity(highScores);
				finish();
			}
		});

	}

	@Override
	protected void onStop() {
		if (mBound)
			unbindService(mConnection);
		super.onStop();
	}

	@Override
	protected void onDestroy() {

		playersCursor.close();
		mdba.close();
		unregisterReceiver(r);
		super.onDestroy();
	}

	/** Method needed by the system **/
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/** Receives signal from ClientProtocol that DB has been updated **/
	public class UpdateBroadcastReceiver extends BroadcastReceiver {

		boolean startSignal, update, endSignal;

		@Override
		public void onReceive(Context context, Intent intent) {

			update = intent.getBooleanExtra("update", false);
			if (update) {
				//Log.d(LOG_TAG,
				//		"Game Update BroadcastReceiver received game update");
				playersCursor.requery();
				mapView.invalidate();
				return;
			}

			endSignal = intent.getBooleanExtra("endSignal", false);
			if (endSignal) {
				//Log.d(LOG_TAG,
				//		"Game Update BroadcastReceiver received End Signal");
				// disable screen touches
				playersOverlay.setEndOfGame(true);
				timer.stop();
				endDialog.show();
				return;
			}

			startSignal = intent.getBooleanExtra("startSignal", false);
			if (startSignal) {
				//Log.d(LOG_TAG,
				//		"Game Update BroadcastReceiver received Start Signal");
				startTask = new StartTask();
				startTask.execute();
				return;
			}
		}
	}

	/** Task to start the game with a 3 seconds count down **/
	class StartTask extends AsyncTask<Void, Integer, Void> {

		private final ToneGenerator tg = new ToneGenerator(
				AudioManager.STREAM_NOTIFICATION, 100);
		private final long DELAY = 1200;

		@Override
		protected Void doInBackground(Void... params) {
			int i = 3;

			while (i >= 0) {
				if (this.isCancelled())
					break;
				publishProgress(i);
				try {
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i--;
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
			tvCountdown.setText("" + progress[0]);
		}

		@Override
		protected void onPostExecute(Void result) {
			rl.removeView(tvCountdown);
			timer.setBase(SystemClock.elapsedRealtime());
			timer.start();
			playersOverlay.setGameStarted(true); // enable screen touches
		}
	}

	@Override
	public void onBackPressed() {

		// Create the dialog for when user presses the back button during the
		// game
		// using the Activity's context to show up correctly
		ctw = new ContextThemeWrapper(mContext,
				android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
		backDialog = new AlertDialog.Builder(ctw);
		backDialog.setTitle("Exit Game ?");
		backDialog.setMessage("Are you sure you want to leave the game?");
		backDialog.setCancelable(false);
		backDialog.setPositiveButton("Yes", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent i = new Intent(iEARLY_QUIT);
				mContext.sendBroadcast(i);
				startTask.cancel(true);
				finish();
			}
		});

		backDialog.setNegativeButton("No", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		backDialog.show();
	}

}