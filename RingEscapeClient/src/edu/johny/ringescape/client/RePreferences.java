package edu.johny.ringescape.client;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/** 
 * Activity that shows and handles changes of the User's settings 
 * @author Ioannis Chiotellis
 **/
public class RePreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, AppConstants {

	private SharedPreferences prefs;
	private EditTextPreference namePref, ipPref;
	private ListPreference radiusPref, drawableRadiusPref, portPref;
	private Builder builder;
	private AlertDialog invalidNameDialog, invalidIpDialog;

	private final static String invalidNameMessage = 
			"Sorry, your name cannot contain "
			+ FIELD_DELIM + ", " + REC_DELIM + ", " + UPD_DELIM
			+ ", a newline or begin with a space"
			+ " and it cannot contain more than 12 characters!";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		prefs = PreferenceManager
				.getDefaultSharedPreferences(RePreferences.this);
		prefs.registerOnSharedPreferenceChangeListener(RePreferences.this);

		namePref = (EditTextPreference) findPreference("name");
		String name = namePref.getText();
		namePref.setSummary(name);

		ipPref = (EditTextPreference) findPreference("server_ip");
		String serverIp = ipPref.getText();
		ipPref.setSummary(serverIp);

		radiusPref = (ListPreference) findPreference("radius");
		String radius = radiusPref.getValue();
		radiusPref.setSummary(radius);

		drawableRadiusPref = (ListPreference) findPreference("drawable_player_radius");
		String dpr = drawableRadiusPref.getValue();
		drawableRadiusPref.setSummary(dpr);

		portPref = (ListPreference) findPreference("server_port");
		String sp = portPref.getValue();
		portPref.setSummary(sp);

		builder = new Builder(RePreferences.this);
		builder.setTitle("Invalid Name")
			   .setMessage(invalidNameMessage)
			   .setCancelable(false).setNeutralButton("OK", null);
		invalidNameDialog = builder.create();

		builder = new Builder(RePreferences.this);
		builder.setTitle("Invalid Server IP")
			   .setMessage(this.getString(R.string.invalidIpMessage))
			   .setCancelable(false).setNeutralButton("OK", null);
		invalidIpDialog = builder.create();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		if (key.equals("name")) {
			String name = sharedPreferences.getString("name", namePref.getSummary()
					.toString());
			
			if(nameIsValid(name))
				namePref.setSummary(name);
			else{
				String newName = validateName(name);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.remove("name");
				editor.putString("name", newName);
				editor.commit();
				namePref.setText(newName);
				namePref.setSummary(newName);
				invalidNameDialog.show();
			}

			return;
		}
		

		if (key.equals("server_ip")) {
			String serverIp = sharedPreferences.getString("server_ip", ipPref
					.getSummary().toString());

			boolean serverIpIsValid = validateIP(serverIp);

			if (serverIpIsValid)
				ipPref.setSummary(serverIp);
			else {

				String newServerIp = DEFAULT_SERVER_IP;
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.remove("server_ip");
				editor.putString("server_ip", newServerIp);
				editor.commit();
				ipPref.setText(newServerIp);
				ipPref.setSummary(newServerIp);
				invalidIpDialog.show();
			}

			return;
		}

		if (key.equals("radius")) {
			radiusPref.setSummary(radiusPref.getValue());
			return;
		}

		if (key.equals("drawable_player_radius")) {
			drawableRadiusPref.setSummary(drawableRadiusPref.getValue());
			return;
		}

		if (key.equals("server_port")) {
			portPref.setSummary(portPref.getValue());
		}

	}

	private boolean nameIsValid(String name) {
		if (name.contains(FIELD_DELIM) 
				   || name.contains(REC_DELIM)
				   || name.contains(UPD_DELIM) 				   
				   || name.startsWith(" ")
				   || name.contains("\n")
				   || name.length() > MAX_NAME_LENGTH)
					   return false;				   
		return true;
	}

	private String validateName(String name) {
		String newName = name;
					
		if (name.contains(FIELD_DELIM))
			newName = newName.replaceAll(FIELD_DELIM, "");

		if (name.contains(REC_DELIM))
			newName = newName.replaceAll(REC_DELIM, "");

		if (name.contains(UPD_DELIM))
			newName = newName.replaceAll(UPD_DELIM, "");
		
		while(newName.startsWith(" "))
			newName = newName.replaceFirst(" ", "");
		
		if(name.contains("\n"))
			newName = name.replaceAll("\n", "");

		if (newName.length() > MAX_NAME_LENGTH)
			newName = newName.substring(0, MAX_NAME_LENGTH);

		return newName;
	}

	private boolean validateIP(String ip) {

		if (ip.length() > 15)
			return false;

		String[] splits = ip.split("\\.");
		if (splits.length != 4)
			return false;
		else {
			int i = 0;
			boolean valid = true;
			while (valid && i < splits.length) {
				int var;
				try {
					var = Integer.valueOf(splits[i]);
					if (var < 0 || var > 255)
						valid = false;
				} catch (NumberFormatException nfe) {
					valid = false;
				}
				i++;
			}
			return valid;
		}
	}

}