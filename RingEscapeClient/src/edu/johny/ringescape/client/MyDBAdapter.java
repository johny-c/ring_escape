package edu.johny.ringescape.client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

/** 
 * Class that handles the SQLite Database interactions of the Player 
 * @author Ioannis Chiotellis
 **/
public class MyDBAdapter implements AppConstants{
  
  private static final String DATABASE_NAME = "reDb.db";
  private static int DATABASE_VERSION = 1;
  //MAIN TABLE
  private static final String MAIN_TABLE = "reMain";
   
  public static final String PLAYER_ID = "player_id";	// The name of each column in your database.
  public static final String P_NAME = "name"; 			
  public static final String P_LAT = "latitude";
  public static final String P_LON = "longitude";
  public static final String P_SCORE = "score";
  public static final String P_SERVER_ID = "server_id";
  public static final String P_STATUS = "status";
  public static final String P_HITTER = "hitter";
  
  public static final int NAME_COLUMN = 1;			// The index of each column in the database.
  public static final int LAT_COLUMN = 2;
  public static final int LON_COLUMN = 3; 
  public static final int STATUS_COLUMN = 4;
  public static final int SCORE_COLUMN = 5;
  public static final int PLAYER_ID_COLUMN = 6;
   
  // SQL Statement to create a new database.
  private static final String MAIN_TABLE_CREATE = "create table if not exists " + 
	MAIN_TABLE + " (" + PLAYER_ID + " integer primary key autoincrement, " +
    P_NAME      + " text not null, " +
    P_LAT       + " double not null, " + 
    P_LON       + " double not null, " +
    P_SCORE     + " int not null, " + 
    P_SERVER_ID	+ " int not null, " +
    P_STATUS	+ " int not null, " +
    P_HITTER    + " int not null);";
 
  
  // HIGHSCORES TABLE
  private static final String HIGHSCORES_TABLE = "reHighscores";
  public static final String H_ID = "highscore_id";
  public static final String H_NAME = "h_name";
  public static final String H_SCORE = "h_score";
  public static final int H_NAME_COLUMN = 1;		
  public static final int H_SCORE_COLUMN = 2;
  
  //SQL Statement to create the Highscores table
  private static final String HIGHSCORES_CREATE = "create table if not exists " + 
    HIGHSCORES_TABLE + " (" + H_ID + " integer primary key autoincrement, " +
    H_NAME      + " text not null, " +
    H_SCORE + " int not null);";
 
  private SQLiteDatabase db;		// Variable to hold the database instance  
  private final Context context;	// Context of the application using the database. 
  private myDbHelper dbHelper;		// Database open/upgrade helper
  private int anonymousPlayersCounter = 0;

  
  public MyDBAdapter(Context _context) {
    context = _context;
    dbHelper = new myDbHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public MyDBAdapter open() throws SQLException {
    db = dbHelper.getWritableDatabase();
    return this;
  }

  public void close() {
	  if(db != null)
		  db.close();
  }
  
  
  /** ------------ MY METHODS ------------ **/

  
  /** 1 Insert a new Player from String **/
  long insertPlayer(String record, int myId) {

	  String[] values = record.split(FIELD_DELIM);
	  ContentValues cv = new ContentValues();
	  
	  int id = Integer.valueOf(values[0]);
	  cv.put(P_SERVER_ID, id);
	  
	  String name = values[1]; 
	  if(id != myId && name.equals("You"))
		  cv.put(P_NAME, "Player"+(++anonymousPlayersCounter));
	  else
		  cv.put(P_NAME, name);
	  
	  cv.put(P_LAT, Double.valueOf(values[2]));
	  cv.put(P_LON, Double.valueOf(values[3]));  
	  cv.put(P_SCORE, 0);
	  cv.put(P_STATUS, Player.FREE);
	  cv.put(P_HITTER, 0);
	  
	  try{	  
		  return db.insert(MAIN_TABLE, null, cv);
	  }catch(SQLiteException e){
		  e.printStackTrace();
		  return -1;
	  }	  
  }
  
  
  /** 2 Insert a new Player from Player object **/
  long insertEntry(Player player) {
    // Create a new ContentValues to represent my row
    // and insert it into the database.
	  
	  ContentValues cv = new ContentValues();
	  cv.put(P_NAME, player.getName());
	  cv.put(P_LAT, player.getLatitude());
	  cv.put(P_LON, player.getLongitude());
	  cv.put(P_SERVER_ID, player.getId());
	  cv.put(P_SCORE, player.getScore());
	  cv.put(P_STATUS, Player.FREE);
	  cv.put(P_HITTER, player.getHitter());
	  
	  try{	  
		  return db.insert(MAIN_TABLE, null, cv);
	  }catch(SQLiteException e){
		  e.printStackTrace();
		  return -1;
	  }	  
  }
    
  
  /** 3 Update game according to theInput from Server **/
  void updateGame(String theInput) {
		String[] players = theInput.split(REC_DELIM);
		
		for(String player : players){
			String[] values = player.split(FIELD_DELIM);
			Player p = new Player();
			p.setId(Integer.valueOf(values[0]));
			p.setLatitude(Double.valueOf(values[1]));
			p.setLongitude(Double.valueOf(values[2]));
			p.setStatus(Integer.valueOf(values[3]));				
			p.setScore(Integer.valueOf(values[4]));
			p.setHitter(Integer.valueOf(values[5]));
			updateEntry(p);
		}
  }
	

  
  /** 4 Update player location and status **/
  boolean updateEntry(Player player) {
 
		  ContentValues cv = new ContentValues();
		  cv.put(P_LAT, player.getLatitude());
		  cv.put(P_LON, player.getLongitude());
		  cv.put(P_STATUS, player.getStatus());
		  cv.put(P_SCORE, player.getScore());
		  cv.put(P_HITTER, player.getHitter());

		  try{	  
			  db.update(MAIN_TABLE, cv, P_SERVER_ID + "=" + player.getId(), null);
		  } catch (SQLiteException e) {
			  e.printStackTrace();
			  return false;
		  }
		  return true;
  }
	
	
  /** 5 Get game status to update the map during the game **/
  Cursor getGame() {
	  String[] columns = { P_NAME, P_LAT, P_LON, P_STATUS, P_SCORE, P_SERVER_ID, P_HITTER };
	  
    return db.query(MAIN_TABLE, columns, null, null, null, null, null);
  }


  /** 6 Set High Scores at the end of each game **/
  void setHighScores() {
		// select names and scores from main table
		  String[] columns = { P_NAME, P_SCORE };	

		  Cursor c = db.query(MAIN_TABLE, columns, null, null, null,null,null);		  
		  ContentValues cv = new ContentValues();
			if(c!= null){
				for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
					// insert them into highscores table
					cv.put(H_NAME, c.getString(0));
					cv.put(H_SCORE, c.getInt(1));
					try{
						db.insert(HIGHSCORES_TABLE, null, cv);
					} catch (SQLiteException e) { e.printStackTrace(); }
					cv.clear();
				}		
			}
		c.close();	

		// Count entries of highscores table			
		String amount = "select count(*) from " + HIGHSCORES_TABLE+";";		
	    SQLiteStatement statement = db.compileStatement(amount);
	    long count = statement.simpleQueryForLong();
	    statement.close();
	    // Remove entries from highscores table if more than @TOP_PLAYERS entries
		if(count > TOP_PLAYERS_SHOW){					
			String top = "select " + H_ID + " from " + HIGHSCORES_TABLE + 
				    	 " order by " + H_SCORE + " desc limit 10";
			db.delete(HIGHSCORES_TABLE, H_ID + " not in (" + top + ")", null);				
		}
  }
  
  
  /** 7 Get High Scores for showing in the HighScores Activity **/
  Cursor getHighScores(){
	  String[] columns = {H_NAME, H_SCORE};
	 
	    return db.query(HIGHSCORES_TABLE, columns, 
	                    null, null, null, null, H_SCORE+" desc", " 10");
  }
  
  
  /** 8 Clear the main table **/
  void clearTable(){
	  if(db != null)
		  if(db.isOpen())
			  db.delete(MAIN_TABLE, null, null);
  }
 
  

  private static class myDbHelper extends SQLiteOpenHelper {

    public myDbHelper(Context context, String name, 
                      CursorFactory factory, int version) {
      super(context, name, factory, version);
    }

    // Called when no database exists in disk and the helper class needs
    // to create a new one. 
    @Override
    public void onCreate(SQLiteDatabase _db) {
      _db.execSQL(MAIN_TABLE_CREATE);
      _db.delete(MAIN_TABLE, null, null);
      _db.execSQL(HIGHSCORES_CREATE);
    }

    // Called when there is a database version mismatch meaning that the version
    // of the database on disk needs to be upgraded to the current version.
    @Override
    public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
      // Log the version upgrade.
      //Log.w("TaskDBAdapter", "Upgrading from version " + 
      //                       _oldVersion + " to " +
       //                      _newVersion + ", which will destroy all old data");
        
      // Upgrade the existing database to conform to the new version. Multiple 
      // previous versions can be handled by comparing _oldVersion and _newVersion
      // values.

      // The simplest case is to drop the old table and create a new one.
      _db.execSQL("DROP TABLE IF EXISTS " + MAIN_TABLE);
      _db.execSQL("DROP TABLE IF EXISTS " + HIGHSCORES_TABLE);
      // Create a new one.
      onCreate(_db);
    }
    
  }

		
}