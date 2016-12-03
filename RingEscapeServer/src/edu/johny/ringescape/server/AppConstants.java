package edu.johny.ringescape.server;

/**
 * Interface that contains all constants that are used by the Client and the
 * Server
 * @author Ioannis Chiotellis
 **/
public interface AppConstants {

	/** Client Constants **/
	// times
	static final int MAP_LOADING_TIME = 6000;
	static final int RESTING_TIME = 1000;
	static final int SOCKET_TIMEOUT = 30000;

	// UI parameters
	static final int MAX_NAME_LENGTH = 12;
	static final int TOP_PLAYERS_SHOW = 10;

	// updates
	static final String iANSWER_UPDATE = "au";
	static final String iHIT_UPDATE = "hu";
	static final String iLOCATION_UPDATE = "lu";
	static final String iGAME_UPDATE = "gu";
	static final String iEARLY_QUIT = "eq";
	
	// distances
	static final double REACH_DISTANCE_IN_KM = 0.05;
	
	// server address
	static final String DEFAULT_SERVER_IP = "192.168.1.2";
	
	/** --------------------------------------------------------------------- **/
	/** Common Constants **/
	// message types
	static final int INITIAL = 0; // CLIENT -> SERVER
	static final int ANSWER_UPDATE = 1; // SERVER -> CLIENT
	static final int ANSWER_RECEIVED = 1; // CLIENT -> SERVER
	static final int FINAL_ANSWER = 2; // SERVER -> CLIENT
	static final int FINAL_RECEIVED = 2; // CLIENT -> SERVER
	static final int INIT_GAME = 3; // SERVER -> CLIENT
	static final int READY = 3; // CLIENT -> SERVER
	static final int START_GAME = 4; // SERVER -> CLIENT
	static final int PLAYER_UPDATE = 4; // CLIENT -> SERVER
	static final int GAME_UPDATE = 5; // SERVER -> CLIENT

	static final int EXIT = 8; // SERVER <-> CLIENT (CLOSING SOCKET)
	static final int NO_PLAYERS_FOUND = 80; // SERVER -> CLIENT
	static final int END_OF_GAME = 81; // SERVER -> CLIENT
	static final int SERVER_TOO_BUSY = 82; // SERVER -> CLIENT
	static final int SERVER_DOWN = 83; // JUST CLIENT SIDE
	static final int EARLY_QUIT = 84; // CLIENT -> SERVER
	static final int UNKNOWN_MESSAGE_TYPE = 85; // SERVER <-> CLIENT
	static final int SET_VAR = 86; // CLIENT -> SERVER
	static final int UNKNOWN_SERVER = 91;
	static final int SOCKET_EXCEPTION = 92;

	// delimiters
	static final String FIELD_DELIM = "@";
	static final String REC_DELIM = "#";
	static final String UPD_DELIM = "%";

	// distances
	//static final double ESCAPE_DISTANCE_IN_KM = 0.05;

	// points
	static final int ESCAPE_REWARD_POINTS = 100;
	static final int ATTACK_REWARD_POINTS = 50;

	// times
	static final int PRISON_TIME = 20000;

	/** --------------------------------------------------------------------- **/
	/** Server Constants **/
	// server
	static final int SERVER_PORT = 12345;	// forwarded from port 80
	static final int MAX_CLIENTS_IN_QUEUE = 20;
	static final int MAX_PLAYERS_PER_GAME = 10;

	// times
	static final int SLEEPING_TIME = 5000; // 5 seconds
	static final int MAX_SEARCHING_TIME = 3 * SLEEPING_TIME; // 15 seconds
	static final int CLIENT_TIME_OUT = 30000;
	static final int COUNTDOWN_TIME = 4000;
	static final int GAME_DURATION = 2 * 60 * 1000 + COUNTDOWN_TIME; // 2 minutes
	static final int ESCAPE_TIME = 10000;

}
