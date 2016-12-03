package edu.johny.ringescape.client;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Launcher Activity that shows possible choices for the User
 * 
 * @author Ioannis Chiotellis
 **/
public class MainMenu extends ListActivity {

	private static String[] menuChoices;
	SharedPreferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		menuChoices = getResources().getStringArray(
				R.array.main_menu);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setAdapter(new IconicAdapter());
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent,
					View view, int position, long id) {
				// When clicked, show a toast with the TextView text
				TextView tv = (TextView) view
						.findViewById(R.id.label);
				Toast.makeText(getApplicationContext(),
						tv.getText(), Toast.LENGTH_SHORT).show();

				Intent i;

				switch (position) {
				case 0:

					if (!internetIsEnabled()) {
						AlertDialog ad = new AlertDialog.Builder(
								MainMenu.this).create();
						ad.setTitle(R.string.connectionErrorTitle);
						ad.setMessage(getString
							(R.string.connectionErrorMessage));
						ad.show();
						break;
					}


					boolean virtual = prefs.getBoolean("virtual",
							false);
					
					if(virtual && !mockLocationsEnabled()){
						AlertDialog ad = new AlertDialog.Builder(
								MainMenu.this).create();
						ad.setTitle(R.string.mockErrorTitle);
						ad.setMessage(getString
							(R.string.mockErrorMessage));
						ad.show();
						break;
					}

					if (!virtual && !gpsIsEnabled()) {
						AlertDialog ad = new AlertDialog.Builder(
								MainMenu.this).create();
						ad.setTitle(R.string.providerErrorTitle);
						ad.setMessage(getString
							(R.string.providerErrorMessage));
						ad.show();
						break;
					}

					i = new Intent(MainMenu.this, NewGame.class);
					startActivity(i);
					break;

				case 1:
					i = new Intent(MainMenu.this,
							HighScores.class);
					startActivity(i);
					break;

				case 2:
					i = new Intent(MainMenu.this,
							RePreferences.class);
					startActivity(i);
					break;

				case 3:
					i = new Intent(MainMenu.this,
							Instructions.class);
					startActivity(i);
					break;

				case 4:
					i = new Intent(MainMenu.this, AppInfo.class);
					startActivity(i);
					break;

				default:
					break;
				}

			}

			private boolean mockLocationsEnabled() {
				String mock = Settings.Secure.getString(
						getContentResolver(), 
						Settings.Secure.ALLOW_MOCK_LOCATION);
				if(mock != null){
					return mock.equals("1");
				}
				return false;
			}

			private boolean gpsIsEnabled() {
				String provider = Settings.Secure.getString(
						getContentResolver(), 
						Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
				if(provider != null){
					return provider.contains(LocationManager.GPS_PROVIDER);
				}
				return false;
				/*
				LocationManager lm = (LocationManager) 
					getSystemService(LOCATION_SERVICE);
				String provider = LocationManager.GPS_PROVIDER;
				return lm.isProviderEnabled(provider);
				*/
			}

			private boolean internetIsEnabled() {
				ConnectivityManager cm = (ConnectivityManager) 
					getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo ni = cm.getActiveNetworkInfo();

				if ((ni != null) && ni.isConnected())
					return true;
				else
					return false;
			}

		});

	}

	/** Adapter to show menu icons and menu labels **/
	public class IconicAdapter extends ArrayAdapter<String> {
		IconicAdapter() {
			super(MainMenu.this, R.layout.mainmenu_row,
					R.id.label, menuChoices);
		}

		@Override
		public View getView(int position, View convertView,
				ViewGroup parent) {

			View row = super.getView(position, convertView, parent);
			ImageView icon = (ImageView) row.findViewById(R.id.icon);

			switch (position) {

			case 0:
				icon.setImageResource(R.drawable.ic_menu_new_game);
				break;
			case 1:
				icon.setImageResource(R.drawable.ic_menu_highscores);
				break;
			case 2:
				icon.setImageResource(R.drawable.ic_menu_prefs);
				break;
			case 3:
				icon.setImageResource(R.drawable.ic_menu_instructions);
				break;
			case 4:
				icon.setImageResource(R.drawable.ic_menu_appinfo);
				break;

			default:
				break;
			}

			return (row);
		}
	}

}