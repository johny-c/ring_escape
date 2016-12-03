package edu.johny.ringescape.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.widget.Toast;

/** 
 * AsyncTask that provides mock locations (used during development) 
 * @author Ioannis Chiotellis
 **/
public class MockLocationProviderTask extends AsyncTask<Void, Void, Void> {
	
	//private final String LOG_TAG = this.getClass().getSimpleName();
    private LocationManager locationManager;
    private String mockLocationProvider;
    private String mockDataFileName;
    private Context context;
    private List<String> mockData;    

    public MockLocationProviderTask(LocationManager locationManager,
            String mockLocationProvider, String mockDataFileName, Context context) throws IOException {
    
        this.locationManager = locationManager;
        this.mockLocationProvider = mockLocationProvider;
        this.mockDataFileName = mockDataFileName;
        this.context = context;
    }

    @Override
	protected Void doInBackground(Void... params) {
    	
        mockData = new ArrayList<String>();
        
        try{
    	InputStream is = context.getAssets().open(mockDataFileName);
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	// add each line in the file to the list
    	String line = null;
    		while ((line = reader.readLine()) != null) {
    			mockData.add(line);
    		}
    	} catch (IOException e) {
    		String str = "Problem with mock location data file !\n";
    		Toast.makeText(context, str+e.toString(), Toast.LENGTH_LONG);
    		e.printStackTrace();
    	}
    			
        for (String str : mockData) {
        	
            if(this.isCancelled())
            	break;
        	
            try {

                Thread.sleep(1000);

            } catch (InterruptedException e) {
            	//Log.d(LOG_TAG, "Sleeping thread interrupted !");           	
                e.printStackTrace();
                break;
            }
            

            // Set one position
            String[] parts = str.split(";");
            Double latitude = Double.valueOf(parts[0]);
            Double longitude = Double.valueOf(parts[1]);
            //Double altitude = Double.valueOf(parts[2]);
            Location location = new Location(mockLocationProvider);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            //location.setAltitude(altitude);

            //Log.i(LOG_TAG, location.toString());
            // set the time in the location. If the time on this location
            // matches the time on the one in the previous set call, it will be
            // ignored
            location.setTime(System.currentTimeMillis());

            locationManager.setTestProviderLocation(mockLocationProvider,
                    location);
            if(this.isCancelled())
            	break;
        }
        
		return null;
	}

}
