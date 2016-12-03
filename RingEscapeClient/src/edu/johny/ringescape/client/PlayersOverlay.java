package edu.johny.ringescape.client;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

/**
 * Class that extends ItemizedOverlay to handle the players drawing on the map 
 * @author Ioannis Chiotellis
 **/
public class PlayersOverlay extends ItemizedOverlay<OverlayItem> 
								implements AppConstants {

	//private final String LOG_TAG = this.getClass().getSimpleName();
	private final int DRAWABLE_PLAYER_RADIUS;
	private Context mContext;
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private GeoPoint geoPoint = new GeoPoint(38002859, 23675988);
	// teiath (Some initial GeoPoint could be (0,0)
	private Cursor playersCursor; // Cursor from local DB
	private ArrayList<Player> playersList; // List to hold values before update
	private Player player;
	private boolean gameStart = false, endOfGame = false;
	long gameStartTime, tapTime, hitTime;
	Point hitPoint;

	public PlayersOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		mContext = context;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		DRAWABLE_PLAYER_RADIUS = Integer.valueOf(prefs.getString(
				"drawable_player_radius", "10"));
		populate();
	}

	public PlayersOverlay(Player player, Cursor cursor, Drawable drawable,
			Context context) {
		this(drawable, context);
		this.player = player;
		playersCursor = cursor;
		//Log.d(LOG_TAG, "My cursor has " + cursor.getCount() + " rows");
		playersList = null;
		playersList = new ArrayList<Player>();
		// create local playersList to be used in between game updates
		initPlayersList();
		playersCursor.registerDataSetObserver(new DataSetObserver() {
			@Override
			// called on Cursor.requery();
			public void onChanged() {
				refreshPlayersList();
			}
		});
	}

	/** Initializes the local Players List **/
	public void initPlayersList() {

		if (playersCursor.moveToFirst())
			do {
				Player p = new Player();
				p.setName(playersCursor.getString(0));
				p.setLatitude(playersCursor.getDouble(1));
				p.setLongitude(playersCursor.getDouble(2));
				p.setStatus(playersCursor.getInt(3));
				p.setScore(playersCursor.getInt(4));
				p.setId(playersCursor.getInt(5));
				//Log.d(LOG_TAG, "INIT: " + p.getName() + " " + p.getLatitude()
				//		+ " " + p.getLongitude() + " " + p.getStatus() + " "
				//		+ p.getId() + " HA");
				playersList.add(p);
			} while (playersCursor.moveToNext());
	}

	/**
	 * Refreshes the local Players List when a new update from Server has been
	 * received
	 **/
	public void refreshPlayersList() {
		int i = 0;
		Player p;
		String newName;
		double newLat, newLon;
		int newStatus, newScore, newId, newHitter;

		if (playersCursor.moveToFirst())
			do {
				p = playersList.get(i);

				newName = playersCursor.getString(0);
				newLat = playersCursor.getDouble(1);
				newLon = playersCursor.getDouble(2);
				newStatus = playersCursor.getInt(3);
				newScore = playersCursor.getInt(4);
				newId = playersCursor.getInt(5);
				newHitter = playersCursor.getInt(6);

				// Toasts regarding self-player status
				if (p.getId() == player.getId()) {

					if (p.getStatus() == Player.THREATENED
							&& newStatus == Player.FREE)
						toast("YOU ESCAPED !   (+" + ESCAPE_REWARD_POINTS + ")");
					else if (newStatus == Player.THREATENED
							&& p.getStatus() == Player.FREE)
						toast("You are now threatened!  R U N !");
					else if (newStatus == Player.IMPRISONED
							&& p.getStatus() == Player.THREATENED)
						toast("You are now in prison...");
				}

				// Toasts regarding other players hit by the self-player
				if (newStatus == Player.THREATENED
						&& p.getStatus() == Player.FREE) {
					// set the Ring
					Location location = new Location("");
					location.setLatitude(p.getLatitude());
					location.setLongitude(p.getLongitude());
					p.setRingCenter(location);
					p.setCorrectingFactor(1 / Math.cos(Math.toRadians(location
							.getLatitude())));

					if (newHitter == player.getId())
						toast("You just hit " + p.getName() + " !");
				} else if (newStatus == Player.IMPRISONED
						&& p.getStatus() == Player.THREATENED) {
					p.setRingCenter(null);
					if (p.getHitter() == player.getId())
						toast("You put " + p.getName() + " in prison!  (+"
								+ ATTACK_REWARD_POINTS + ")");
				} else if (newStatus == Player.FREE
						&& p.getStatus() == Player.THREATENED) {
					p.setRingCenter(null);
					if (p.getHitter() == player.getId())
						toast(p.getName() + " ESCAPED !");
				} else if (newStatus == Player.FREE
						&& p.getStatus() == Player.IMPRISONED
						&& p.getHitter() == player.getId())
					toast(p.getName() + " is RELEASED !");

				p.setName(newName);
				p.setLatitude(newLat);
				p.setLongitude(newLon);
				p.setStatus(newStatus);
				p.setScore(newScore);
				p.setId(newId);
				p.setHitter(newHitter);
				//Log.d(LOG_TAG, "YE " + p.getName() + " " + p.getLatitude()
				//		+ " " + p.getLongitude() + " " + p.getStatus() + " "
				//		+ p.getId() + " " + " HA");

				playersList.set(i, p); // replace entry - do not delete/insert
				i++;
			} while (playersCursor.moveToNext());

	}

	/** Default required methods of ItemizedOverlay **/
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	/** Must be overriden and return true 
	 * in order for the other onTap to work 
	 **/
	@Override
	protected boolean onTap(int index) {
		return true;
	}

	/** All hits are first recorded here **/
	@Override
	public boolean onTap(final GeoPoint hitGeoPoint, final MapView mapView) {

		tapTime = System.nanoTime();
		//Log.d(LOG_TAG, "Entering onTap method at = " + tapTime);
		if (!gameStarted()) {
			toast("Game has not started yet!");
			//Log.d(LOG_TAG,
			//		"onTap has been called but game has not started yet!");
			return true;
		}

		if (isEndOfGame()) {
			//Log.d(LOG_TAG, "onTap has been called but game has ended!");
			return true;
		}

		// find the self player first
		Player me = null;
		boolean foundMe = false;
		Iterator<Player> itr = playersList.iterator();
		while (itr.hasNext() && !foundMe) {
			Player p = itr.next();
			if (p.getId() == player.getId()) {
				//Log.d(LOG_TAG, "playerStatus known = " + p.getStatus());
				me = p;
				foundMe = true;
			}
		}

		//Log.d(LOG_TAG, "PlayersList size: " + playersList.size());
		// Check if the player is in prison, so he cannot hit others
		if (me.getStatus() == Player.IMPRISONED) {
			toast("You are in prison ! \n You cannot hit others now.");
			return true;
		}

		// Convert hitpoint geo-coordinates to screen-coordinates
		Projection projection = mapView.getProjection();
		hitPoint = new Point();
		projection.toPixels(hitGeoPoint, hitPoint);
		hitTime = System.currentTimeMillis();

		// First check if the player hit himself
		Point meScreenPoint = new Point();
		GeoPoint meGeoPoint = new GeoPoint((int) (me.getLatitude() * 1E6),
				(int) (me.getLongitude() * 1E6));
		projection.toPixels(meGeoPoint, meScreenPoint);

		if ((Math.abs(hitPoint.x - meScreenPoint.x) <= DRAWABLE_PLAYER_RADIUS)
		&& (Math.abs(hitPoint.y - meScreenPoint.y) <= DRAWABLE_PLAYER_RADIUS)){
			toast("Don't hit yourself");
			//Log.d(LOG_TAG, "onTap: Player hit himself!");
			return true;
		}

		// Then check if the player did hit another player
		Iterator<Player> it = playersList.iterator();
		boolean foundHit = false;
	while (it.hasNext() && !foundHit) {

		Player player = it.next();
		// if another player is the self-player skip
		if (player.getId() == me.getId())
			continue;

		// get the point of another player on screen from the List
		Point screenPoint = new Point();
		GeoPoint playerPoint = new GeoPoint(
				(int) (player.getLatitude() * 1E6),
				(int) (player.getLongitude() * 1E6));
		projection.toPixels(playerPoint, screenPoint);

		/** if the hit is valid **/
		if ((Math.abs(hitPoint.x - screenPoint.x) <= DRAWABLE_PLAYER_RADIUS)
		 && (Math.abs(hitPoint.y - screenPoint.y) <= DRAWABLE_PLAYER_RADIUS)){

			switch (player.getStatus()) {

			case Player.FREE:
				/** if the player hit is within the player's reach **/
				double distance = me.distanceTo(player);
				if (distance > REACH_DISTANCE_IN_KM) {
					distance *= 1000;
					String d = String.valueOf(distance);
					int commaPosition = d.indexOf('.');
					if (commaPosition != -1
							&& (d.length() > commaPosition + 2))
						d = d.substring(0, commaPosition + 2);
					toast(player.getName() + " is out of your reach (" + d
							+ " m)!");
					foundHit = true;
				} else {
					long hitTime = tapTime - gameStartTime;
					Intent intent = new Intent(iHIT_UPDATE);
					intent.putExtra("hit", player.getId() + FIELD_DELIM
							+ hitTime);
					mContext.sendBroadcast(intent);
					//Log.d(LOG_TAG, "onTap: Player hit: " + player.getName()
					//		+ " SENDING HIT INTENT");
				}
				break;

			case Player.THREATENED:
				toast(player.getName() + " is already under threat");
				//Log.d(LOG_TAG, "onTap: Player hit: " + player.getName()
				//		+ " but already hit!");
				break;

			case Player.IMPRISONED:
				toast(player.getName() + " is already in prison!");
				//Log.d(LOG_TAG, "onTap: Player hit: " + player.getName()
				//		+ " but already in prison!");
				break;

			case Player.QUITTED:

			case Player.EXITED:
				toast(player.getName() + " has exited!");
				break;

			default:
				break;
			}
			foundHit = true;
		}
	} // end of for loop (checks for every player)

	//Log.d(LOG_TAG,
	//		"Exiting onTap method, total time = "
	//				+ (System.nanoTime() - tapTime) + " ns");
	return true;
	}

	/**
	 * Draws the current state of the game according to the local Players List.
	 * Populate() must have been called first. Also called on
	 * mapView.invalidate();
	 * 
	 **/
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// super.draw(canvas, mapView, shadow);
		Projection projection = mapView.getProjection();

		// Create and setup your paint brush
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);

		// Create the circle drawable of every player
		if (!shadow) {

			Point screenPoint = new Point();

			for (Player p : playersList) {

				geoPoint = locationToGeoPoint(p.getLatitude(), 
						p.getLongitude());
				projection.toPixels(geoPoint, screenPoint);

				switch (p.getStatus()) {
				case Player.FREE:
					paint.setARGB(250, 0, 100, 0);
					break;

				case Player.THREATENED:
					paint.setColor(Color.BLUE);
					paint.setStyle(Paint.Style.STROKE);
					paint.setStrokeWidth(2);
					// formula to convert distance to pixels 
					//not only near the equator
					if (p.getRingCenter() != null) {
						double ringLatitude = p.getRingCenter().getLatitude();
						int ringScreenRadius = (int) (projection
								.metersToEquatorPixels((float) 
										ESCAPE_DISTANCE_IN_KM * 1000) 
										* p.getCorrectingFactor());
						Point ringScreenCenter = new Point();
						projection.toPixels(
								locationToGeoPoint(ringLatitude, p
										.getRingCenter().getLongitude()),
								ringScreenCenter);
						canvas.drawCircle(ringScreenCenter.x,
								ringScreenCenter.y, ringScreenRadius, paint);
					}

					// reset paint for drawing player status
					paint.setARGB(250, 255, 140, 0);
					paint.setStyle(Paint.Style.FILL);
					break;

				case Player.IMPRISONED:
					paint.setARGB(250, 255, 0, 0);
					break;
				default:
					break;
				}

				canvas.drawCircle(screenPoint.x, screenPoint.y,
						DRAWABLE_PLAYER_RADIUS, paint);
				paint.setARGB(250, 0, 0, 0);
				canvas.drawText(p.getName(), screenPoint.x
						+ DRAWABLE_PLAYER_RADIUS, screenPoint.y, paint);
			}

			if (hitPoint != null)
				if (System.currentTimeMillis() - hitTime < 1000) {
					paint.setARGB(250, 0, 0, 250);
					canvas.drawCircle(hitPoint.x, hitPoint.y, 5, paint);
				}
		}
	}

	/**
	 * Converts a Location to a GeoPoint. 
	 * Returns the generated GeoPoint object.
	 * @return GeoPoint
	 **/
	public GeoPoint locationToGeoPoint(double latitude, double longitude) {
		Double lat = latitude * 1E6;
		Double lon = longitude * 1E6;
		GeoPoint gp = new GeoPoint(lat.intValue(), lon.intValue());
		return gp;
	}

	/** Utility method to quickly toast a string message **/
	public void toast(String string) {
		Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
	}

	/** Sets the gameStart flag **/
	synchronized void setGameStarted(boolean value) {
		gameStart = value;
		gameStartTime = System.nanoTime();
	}

	/** Gets the gameStart flag **/
	synchronized boolean gameStarted() {
		return gameStart;
	}

	/** Sets the end of game flag **/
	synchronized void setEndOfGame(boolean value) {
		endOfGame = value;
	}

	/** Gets the end of game flag **/
	synchronized boolean isEndOfGame() {
		return endOfGame;
	}

}
