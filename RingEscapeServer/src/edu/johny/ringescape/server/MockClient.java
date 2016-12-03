package edu.johny.ringescape.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Random;


/** 
 * Fake Client communication class used during development
 * @author Ioannis Chiotellis 
 **/
public class MockClient implements AppConstants{
	
	final Socket socket;
	PrintWriter writer;
	BufferedReader reader;
	final String name;
	final int radius;
	final double lat, lon;
	double la, lo;
	int playerId, guc = 0, auc = 0;
	Random r = new Random();
	String locUpd, hitUpd;
	Utils u = new Utils();
	
	MockClient(String n, int rad, double lat, double lon) throws UnknownHostException, IOException {
		this.name = n;
		this.radius = rad;
		this.lat = lat + r.nextDouble()*0.0001 - 0.00005;
		this.lon = lon + r.nextDouble()*0.0001 - 0.00005;
		
		
		
    	socket = new Socket("localhost", SERVER_PORT);
    	writer = new PrintWriter(socket.getOutputStream(), true); 
    	reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	public String processInput(String msgIn) throws SQLException {
        String theOutput = null;
        if(msgIn == null)
        	return "C theInput = null";
        
        int messageType = UNKNOWN_MESSAGE_TYPE;
        try{
        	messageType = Integer.valueOf(msgIn.substring(0, 1));
        }catch(NumberFormatException e){
        	theOutput = "NFE Client Protocol, unknown message type";
        	e.printStackTrace();
        }
 
        String msgContent = msgIn.substring(1);
        
        switch(messageType){
        
        	case ANSWER_UPDATE: 	u.log("received ANSWER UPDATE "+(++auc)+":"+msgContent);
		        			    	theOutput = ANSWER_RECEIVED+"";
		        			    	u.log("sends ANSWER_RECEIVED "+theOutput);
		        				    break;
        				   
        	case FINAL_ANSWER:  	u.log("received FINAL ANSWER "+msgContent);					
        								theOutput = FINAL_RECEIVED+"";        						      								
		        					break;
		        					
        	case INIT_GAME:			String[] info = msgContent.split(REC_DELIM);
        							playerId = Integer.valueOf(info[0]);
        							String update ="";
        							for(int i=1; i<info.length; i++)
        								update += info[i];
        							u.log("received INIT_GAME"); 
        							u.log("MY PLAYERID = "+playerId); 
        							u.log("INIT_GAME: "+update); 
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									
									theOutput = READY+"";									
        							break;
        							
        							
        	case START_GAME:		// send start signal to RingEscape
									la = lat + r.nextDouble()*0.0001 - 0.00005;
									lo = lon + r.nextDouble()*0.0001 - 0.00005;
									locUpd = la+FIELD_DELIM+lo;
									theOutput = PLAYER_UPDATE + locUpd;
									break;
        		
        					 
        	case GAME_UPDATE:		u.log("received GAME UPDATE "+(++guc)+": "+msgContent); 
        							if(guc == 5 && name.equals("Fred")){
        								return EARLY_QUIT+""+messageType;
        							}
        							String[] gu = msgContent.split(REC_DELIM);
        							
        							for(String p : gu){
        								String[] pinfo = p.split(FIELD_DELIM);
        								u.log("Player with Id: "+pinfo[0]
        											+"\nLatitude: "+pinfo[1]
        											+"\nLongitude: "+pinfo[2]
        											+"\nStatus: "+pinfo[3]+"\n");
        							}
        							
									la += r.nextDouble()*0.0001 - 0.00005;
									lo += r.nextDouble()*0.0001 - 0.00005;
									locUpd = la+FIELD_DELIM+lo;
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									theOutput = PLAYER_UPDATE+""+locUpd;        							        											
        							break;       						
        					       	
        	case EXIT:				u.log(name+" received EXIT : "+msgContent);
        							if(msgIn.equals(NO_PLAYERS_FOUND+""))
        								u.log("received NO_PLAYERS_FOUND");
        							else if(msgIn.equals(END_OF_GAME+""))
        								u.log("received End of game");     							
        							else if(msgIn.equals(SERVER_TOO_BUSY+""))
        								u.log("received SERVER TOO BUSY");
        							else if(msgIn.equals(SERVER_DOWN+""))
        								u.log("received SERVER DOWN");

        							theOutput = EXIT+"";  
        							u.log("sends EXIT Bye");
        							break;
        							    							
        	case UNKNOWN_MESSAGE_TYPE:
        				   
        	default: 				theOutput = UNKNOWN_MESSAGE_TYPE+" C wtf umt!";
        							break;
        				
        }
        
        return theOutput;
    }

	void init() throws IOException, InterruptedException, SQLException {
		
		String msg0 = INITIAL+name+FIELD_DELIM+radius+FIELD_DELIM+lat+FIELD_DELIM+lon;
		writer.println(msg0);	
		
		String inputLine, outputLine;	
   	    while ((inputLine = reader.readLine()) != null) {             	    	
	    	Thread.sleep(1000);			// Wait 1 sec
			outputLine = processInput(inputLine); // process message from server 
			
			if(outputLine.equals(EXIT+""))
				break;
			
			writer.println(outputLine);	// respond to server
			
			if (outputLine.startsWith(EXIT+""))	{
				u.log("MC EXITING...");
				break;	// exit while listening loop
			}
			    
	    }
	}
	
}
