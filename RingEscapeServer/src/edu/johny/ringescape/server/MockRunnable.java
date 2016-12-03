package edu.johny.ringescape.server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

/** 
 * Fake Client class used during development 
 * @author Ioannis Chiotellis
 **/
public class MockRunnable implements Runnable {

   	String name;
   	int delay;
   	static Random r = new Random();
   	Utils u = new Utils();
   	MockClient mc;
   	
	MockRunnable(String name){
		this.name = name;
		delay = r.nextInt(5000);
	}
	
	@Override
	public void run(){
		
		
		
		try{
			Thread.sleep(delay);
			
			mc = new MockClient(name, 5, 38.003907, 23.770956);
        	u.log("MOCKCLIENT, "+name+" CREATED");
			mc.init();
		}
		catch (IOException ioe){
			ioe.printStackTrace();
		}
		catch (InterruptedException ie){
			ie.printStackTrace();
		}
		catch (SQLException sqle){
			sqle.printStackTrace();
		}
		u.log("MOCKCLIENT, "+name+" TERMINATED");
	}

}
