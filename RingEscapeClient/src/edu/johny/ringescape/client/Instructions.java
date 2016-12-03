package edu.johny.ringescape.client;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/** 
 * Activity that shows instructions on how to play the game 
 * @author Ioannis Chiotellis
 **/
public class Instructions extends Activity implements AppConstants {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.instructions);

		TextView tvInstructions = (TextView) findViewById(R.id.tv_instructions);

		String text = "Whenever a player enters your radius, you are able to "
				+ "threaten him with imprisonment. Tap on him first and he will have "
				+ (int) (ESCAPE_TIME / 1000F)
				+ " seconds to escape from the Ring placed around him. "
				+ "If he fails to do so, he will get in prison for "
				+ (int) (PRISON_TIME / 1000F)
				+ " seconds."
				+ "But be careful. Players who are free are also able to threaten you!";

		tvInstructions.setText(text);
	}

}
