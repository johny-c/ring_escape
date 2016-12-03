package edu.johny.ringescape.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Handles all interactions with the MySQL Database
 * @author Ioannis Chiotellis 
 **/
public class ServerDatabase implements AppConstants {

	private final static String url = "jdbc:mysql://localhost:3306/";
	private final static String DB_NAME = "ringescape_server_db";
	private final static String driver = "com.mysql.jdbc.Driver";
	private final static String userName = "johny";
	private final static String password = "123";
	private Connection connection = null;
	private ResultSet resultSet = null;
	private PreparedStatement preparedStatement = null;
	Utils u = new Utils();

	/** 1 Open the DataBase **/
	void connect() {

		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName(driver).newInstance();
			// Connect
			connection = DriverManager.getConnection(url + DB_NAME, userName,
					password);
			u.log("-Connected to the database\n");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 2 Close the DataBase **/
	void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (preparedStatement != null) {
				preparedStatement.close();
			}
			// Disconnect
			if (connection != null) {
				connection.close();
				u.log("-Disconnected from database\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/** 3 Insert a new game and return its id **/
	int insertGame(int players) throws SQLException {
		String str = "INSERT INTO GAMES (g_players) VALUES (" + players + ")";
		PreparedStatement st = connection.prepareStatement(str,
				Statement.RETURN_GENERATED_KEYS);
		st.executeUpdate();
		ResultSet rs = st.getGeneratedKeys();
		int g_id = -1;
		while (rs.next()) {
			g_id = rs.getInt(1);
		}
		st.close();

		return g_id;
	}

	/** 4 Insert a new player with his game(id) and return his id **/
	int insert(Player p) throws SQLException {

		String str = "INSERT INTO PLAYERS (p_name, p_radius, p_lat, p_lon, p_offset, p_status, p_game_id) VALUES(?,?,?,?,?,?,?)";
		PreparedStatement st = connection.prepareStatement(str,
				Statement.RETURN_GENERATED_KEYS);
		st.setString(1, p.getName());
		st.setInt(2, p.getRadius());
		st.setDouble(3, p.getLatitude());
		st.setDouble(4, p.getLongitude());
		st.setLong(5, p.getOffset());
		st.setInt(6, Player.WAITING);
		st.setInt(7, p.getGameId());
		st.executeUpdate();

		ResultSet rs = st.getGeneratedKeys();
		int p_id = -1;
		while (rs.next()) {
			p_id = rs.getInt(1);
		}
		st.close();

		return p_id;
	}

	/** 5 Get how many players are in a game **/
	int getPlayersInGame(int gameId) throws SQLException {
		String str = "SELECT g_players FROM GAMES WHERE game_id=" + gameId + "";
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(str);

		int players = 0;
		while (rs.next()) {
			players = rs.getInt(1);
		}
		st.close();
		return players;
	}

	/** 6 Initialize the status of all players to FREE **/
	void initStatuses(int game_id) throws SQLException {
		String str = "UPDATE PLAYERS SET p_status=? WHERE p_game_id=? AND p_status!=?";
		PreparedStatement st = connection.prepareStatement(str);
		st.setInt(1, Player.FREE);
		st.setInt(2, game_id);
		st.setInt(3, Player.QUITTED);
		st.executeUpdate();
		st.close();
	}

	/** 7 Update a player's location according to client's message **/
	void updateLocation(int id, String update) throws SQLException {
		String str = "UPDATE PLAYERS SET p_lat=?, p_lon=? WHERE player_id=?";

		String[] loc = update.split(FIELD_DELIM);
		PreparedStatement st = connection.prepareStatement(str);
		st.setDouble(1, Double.valueOf(loc[0]));
		st.setDouble(2, Double.valueOf(loc[1]));
		st.setInt(3, id);
		st.executeUpdate();
		st.close();
	}

	/** 8 Get a player's status to resolve hit conflicts **/
	int selectStatus(int id) throws SQLException {

		String str = "SELECT p_status FROM PLAYERS WHERE player_id=" + id + "";
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(str);
		int status = 0;

		while (rs.next()) {
			status = rs.getInt(1);
		}
		st.close();
		return status;
	}

	/**
	 * 9 Update a player's status to THREATENED and store his hitter for
	 * possible award
	 **/
	void storeThreat(int hitter_id, int player_id) throws SQLException {
		u.log("*************STORING THREAT + hitterID = " + hitter_id
				+ "  victimID = " + player_id);
		String str = "UPDATE PLAYERS SET p_status=?, p_hitter=? WHERE player_id=?";
		PreparedStatement st = connection.prepareStatement(str);
		st.setInt(1, Player.THREATENED);
		st.setInt(2, hitter_id);
		st.setInt(3, player_id);
		st.executeUpdate();
		st.close();
	}

	/** 10 Update a player's status **/
	void updateStatus(int player_id, int new_status) throws SQLException {

		String str = "UPDATE PLAYERS SET p_status=? WHERE player_id=?";
		PreparedStatement st = connection.prepareStatement(str);
		st.setInt(1, new_status);
		st.setInt(2, player_id);
		st.executeUpdate();
		st.close();
	}

	/** 11 Update a player's score **/
	void updateScore(int player_id, int points) throws SQLException {
		String str = "UPDATE PLAYERS SET p_score=p_score+? WHERE player_id=?";

		PreparedStatement st = connection.prepareStatement(str);
		st.setInt(1, points);
		st.setInt(2, player_id);
		st.executeUpdate();
		st.close();
	}

	/** 12 Get a game update to send back to the clients **/
	String getGameUpdate(int game_id) throws SQLException {
		String str = "SELECT player_id, p_lat, p_lon, p_status, p_score, p_hitter "
				+ "FROM PLAYERS WHERE p_game_id="
				+ game_id
				+ " AND p_status!="
				+ Player.EXITED + "";
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(str);

		String result = "";
		while (rs.next()) {
			int id = rs.getInt(1);
			result += id + FIELD_DELIM;
			result += rs.getDouble(2) + FIELD_DELIM;
			result += rs.getDouble(3) + FIELD_DELIM;
			int status = rs.getInt(4);
			result += status + FIELD_DELIM;
			if (status == Player.QUITTED)
				updateStatus(id, Player.EXITED);
			result += rs.getInt(5) + FIELD_DELIM;
			result += rs.getInt(6) + REC_DELIM;
		}
		st.close();

		if (result.endsWith(REC_DELIM))
			result = result.substring(0, result.length() - 1);

		return result;
	}

	/** 13 Get a player's location to check if he has escaped **/
	double[] getLocation(int player_id) throws SQLException {
		String str = "SELECT p_lat, p_lon FROM PLAYERS WHERE player_id="
				+ player_id + "";
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(str);

		double[] result = new double[2];
		while (rs.next()) {
			result[0] = rs.getDouble(1);
			result[1] = rs.getDouble(2);
		}
		st.close();

		return result;
	}


	/** 14 Delete the location data at the end **/
	void deleteLocations(int game_id) throws SQLException {
		String str = "UPDATE PLAYERS SET p_status=?, p_lat=?, p_lon=? WHERE p_game_id=?";

		PreparedStatement st = connection.prepareStatement(str);
		st.setInt(1, Player.EXITED);
		st.setNull(2, Types.DOUBLE);
		st.setNull(3, Types.DOUBLE);
		st.setInt(4, game_id);
		st.executeUpdate();
		st.close();		
	}

}
