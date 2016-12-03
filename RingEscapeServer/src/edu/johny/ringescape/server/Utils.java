package edu.johny.ringescape.server;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/** 
 * Utility class to print log messages during Server actions 
 * @author Ioannis Chiotellis
 **/
public class Utils {

	final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

	public void log(String string) {

		Calendar cal = Calendar.getInstance();
		String time = sdf.format(cal.getTime());

		String name = Thread.currentThread().getName();
		int padlen = 20 - name.length();
		String pad = "";
		for (int i = 0; i < padlen; i++)
			pad += " ";

		System.out.println(time + "    " + Thread.currentThread().getName()
				+ pad + string);
	}

}
