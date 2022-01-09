/**
 * 
 */
package org.matsim.analysis.pt;

/**
 * @author Aravind
 *
 */
public class MissingStationPersonCounts {


	private int einsteigerCount;
	private int aussteigerCount;
	
	public int getEinsteigerCount() {
		return einsteigerCount;
	}
	public void setEinsteigerCount() {
		this.einsteigerCount += 1;
	}
	public int getAussteigerCount() {
		return aussteigerCount;
	}
	public void setAussteigerCount() {
		this.aussteigerCount += 1;
	}

}
