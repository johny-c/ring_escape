package edu.johny.ringescape.client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Activity that shows relevant information to the user 
 * while trying to create a new game
 * @author Ioannis Chiotellis
 **/
public class NewGame extends Activity implements AppConstants {

	//private final String LOG_TAG = this.getClass().getSimpleName();
	private RelativeLayout rl;
	private TextView tvHi;
	private TextView tvRadius;
	private ProgressBar progressBar;
	private TextView serverResponse;
	private Chronometer timer;
	private Player player;
	private UpdateBroadcastReceiver r;
	private int updateCounter = 0, answer;
	private Context mContext;
	private SharedPreferences prefs;
	private long lastPause;
	private String text;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searching_splash);
		mContext = NewGame.this;

		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		rl = (RelativeLayout) findViewById(R.id.relative_layout_splash);
		tvHi = (TextView) findViewById(R.id.tv_pref_name);
		timer = (Chronometer) findViewById(R.id.tv_timer);
		tvRadius = (TextView) findViewById(R.id.tv_pref_radius);
		progressBar = (ProgressBar) findViewById(R.id.progressbar);
		serverResponse = (TextView) findViewById(R.id.tv_others_found);
		rl.setBackgroundColor(Color.DKGRAY);

		player = new Player(prefs.getString("name", "You"));
		player.setRadius(Integer.valueOf(prefs.getString("radius", "1")));

		tvHi.setText("Hey " + player.getName() + " !");
		tvRadius.setText("Searching for other players in " + player.getRadius()
				+ " km radius...");
		tvRadius.setTextColor(Color.GREEN);
		serverResponse.setText("Retrieving your location...");

		Intent intent = new Intent(this, LocationService.class);
		// An Activity may start AND also bind to a service but here we only
		// start it
		startService(intent);

		timer.setBase(SystemClock.elapsedRealtime());
		timer.start();
	}

	@Override
	protected void onStart() {
		IntentFilter filter = new IntentFilter(iANSWER_UPDATE);
		r = new UpdateBroadcastReceiver();
		registerReceiver(r, filter);
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		if (r != null)
			unregisterReceiver(r);

		super.onDestroy();
	}

	/** Receives update from ClientProtocol (Server response) */
	public class UpdateBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.d("NewGame",
			//		"BroadcastReceiver received update from ClientProtocol");
			updateUI(intent);
		}
	}

	/** Updates the user interface according to the intent received **/
	public void updateUI(Intent intent) {

		int state = intent.getIntExtra("state", 0);

		switch (state) {

		case INITIAL:
			//Log.d(LOG_TAG, "Location retrieved");
			text = "Sending your location...";
			serverResponse.setText(text);
			break;

		case ANSWER_UPDATE:
			answer = intent.getIntExtra("answer", 0);
			text = "Players found so far: " + answer + " (update "
					+ (++updateCounter) + ")";
			serverResponse.setText(text);
			break;

		case FINAL_ANSWER:
			timer.stop();
			lastPause = SystemClock.elapsedRealtime();
			answer = intent.getIntExtra("answer", 0);
			tvRadius.setVisibility(View.GONE);
			progressBar.setVisibility(View.GONE);
			if(answer == UNKNOWN_MESSAGE_TYPE)	{
				rl.setBackgroundColor(Color.TRANSPARENT);
				text = intent.getStringExtra("message");
				serverResponse.setText("Message: "+text);	
			}
			else 
				handleFinalAnswer(answer);
			break;

		case INIT_GAME:
			Intent game = new Intent(NewGame.this, RingEscape.class);
			game.putExtra("id", intent.getIntExtra("id", 0));
			game.putExtra("lat", intent.getDoubleExtra("lat", 0));
			game.putExtra("lon", intent.getDoubleExtra("lon", 0));
			startActivity(game);
			finish();
			break;

		default:
			break;

		}
	}

	private void handleFinalAnswer(int answer) {
		
		if(answer > 0 && answer <= 10){
			rl.setBackgroundColor(Color.DKGRAY);

			text = answer + " player";
			if (answer > 1)
				text += "s";
			text += " found !\n Get ready!";
			serverResponse.setText(text);
			return;
		}
	
		rl.setBackgroundColor(Color.TRANSPARENT);
		switch (answer) {

		case NO_PLAYERS_FOUND:
			text = "Sorry, no players found! ";
			if (player.getRadius() < 10)
				text += "\nTry increasing your radius. ";

			final Button retry = new Button(mContext);
			retry.setText("Retry");
			retry.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					retry.setVisibility(View.GONE);
					rl.setBackgroundColor(Color.DKGRAY);
					tvRadius.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.VISIBLE);
					text = "Sending your location...";
					serverResponse.setText(text);
					Intent intent = new Intent(mContext, LocationService.class);
					intent.putExtra("reconnect", true);
					startService(intent);
					timer.setBase(timer.getBase()
							+ SystemClock.elapsedRealtime() - lastPause);
					timer.start();
				}

			});

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			retry.setLayoutParams(params);

			rl.addView(retry, params);
			break;

		case SERVER_TOO_BUSY:
			text = "Sorry, server is too busy! ";
			break;

		case SERVER_DOWN:
			text = "Server is not connected!";
			break;

		case UNKNOWN_SERVER:
			text = "Unknown server! ";
			break;

		case SOCKET_EXCEPTION:
			text = "Socket connection error!";
			break;

		default:
			break;
		}

		serverResponse.setText(text);
	}

	@Override
	public void onBackPressed() {
		Intent i = new Intent(iEARLY_QUIT);
		i.putExtra("earlyQuit", true);
		mContext.sendBroadcast(i);
		//Log.d(LOG_TAG, "Sending early quit");

		super.onBackPressed();
	}

}
