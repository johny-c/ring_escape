package edu.johny.ringescape.server;

/** 
 * Class that contains information about a hit 
 * @author Ioannis Chiotellis
 **/
class HitUpdate implements Comparable<HitUpdate>{

	int hitterId;
	int victimId;
	long timeStamp;
	
	HitUpdate(int hitter, String victim, String time){
		this.hitterId = hitter;
		this.victimId = Integer.valueOf(victim);
		this.timeStamp = Long.valueOf(time);
	}

	@Override
	public int compareTo(HitUpdate o) {	
		
		if(this.timeStamp < o.timeStamp)
			return -1;
		else 
			return 1;
	}
	
}
