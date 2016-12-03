package edu.johny.ringescape.client;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;

/** 
 * Activity that shows the top high scores of the games the Player has played 
 * @author Ioannis Chiotellis
 **/
public class HighScores extends Activity {

	private TableLayout tbl;
	private MyDBAdapter mdba;
	private Cursor cursor;
		
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.high_scores);
		tbl = (TableLayout) findViewById(R.id.tbl1);
		
		mdba = new MyDBAdapter(getApplicationContext());
		mdba.open();
		cursor = mdba.getHighScores();					
		
		LayoutParams r = new LayoutParams();
		r.column = 1;
		r.weight = 1;
		
		LayoutParams p = new LayoutParams();
		p.column = 2;
		p.weight = 3;
		p.leftMargin = 20;
		
		LayoutParams h = new LayoutParams();
		h.column = 3;
		h.weight = 6;
		h.rightMargin = 80;
		
		
		TableRow tbre = new TableRow(this);
		tbre.setPadding(0, 15, 0, 0);
		tbl.addView(tbre);
			
		int i = 1;
		if(cursor != null){
			for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
			
				TableRow tbr = new TableRow(this);
				TextView tvRank = new TextView(this);
				tvRank.setLayoutParams(r);
				tvRank.setTextColor(Color.YELLOW);
				tvRank.setTextSize(18);
				tvRank.setText(""+i);
				
				TextView tvPlayer = new TextView(this);
				tvPlayer.setLayoutParams(p);
				tvPlayer.setTextColor(Color.CYAN);
				tvPlayer.setTextSize(18);
				tvPlayer.setText(cursor.getString(0));
				
				TextView tvScore = new TextView(this);
				tvScore.setLayoutParams(h);
				tvScore.setTextColor(Color.RED);
				tvScore.setGravity(Gravity.RIGHT);
				tvScore.setTextSize(18);
				tvScore.setText(""+cursor.getString(1));
				
				tbr.addView(tvRank);
				tbr.addView(tvPlayer);
				tbr.addView(tvScore);
				tbl.addView(tbr);
				i++;
			}		
		}		
		
		cursor.close();
		mdba.close();
	}	
	
	

	@Override
	protected void onPause() {
		super.onPause();
		this.finish();
	}
	
}