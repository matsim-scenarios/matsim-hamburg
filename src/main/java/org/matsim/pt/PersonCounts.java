/**
 * 
 */
package org.matsim.pt;

/**
 * @author Aravind
 *
 */
public class PersonCounts {

	private int einsteigerOutbound;
	private int aussteigerOutbound;
	private int einsteigerInbound;
	private int aussteigerInbound;
	private int einsteigerOutboundSim = 0;
	private int aussteigerOutboundSim = 0;
	private int einsteigerInboundSim = 0;
	private int aussteigerInboundSim = 0;
	private int einsteigerSim = 0;
	private int aussteigerSim = 0;

	public int getEinsteigerSim() {
		return einsteigerSim;
	}

	public void setEinsteigerSim() {
		this.einsteigerSim += 1;
	}

	public int getAussteigerSim() {
		return aussteigerSim;
	}

	public void setAussteigerSim() {
		this.aussteigerSim += 1;
	}

	public int getEinsteigerOutbound() {
		return einsteigerOutbound;
	}

	public void setEinsteigerOutbound(int einsteigerOutbound) {
		this.einsteigerOutbound = einsteigerOutbound;
	}

	public int getAussteigerOutbound() {
		return aussteigerOutbound;
	}

	public void setAussteigerOutbound(int aussteigerOutbound) {
		this.aussteigerOutbound = aussteigerOutbound;
	}

	public int getEinsteigerInbound() {
		return einsteigerInbound;
	}

	public void setEinsteigerInbound(int einsteigerInbound) {
		this.einsteigerInbound = einsteigerInbound;
	}

	public int getAussteigerInbound() {
		return aussteigerInbound;
	}

	public void setAussteigerInbound(int aussteigerInbound) {
		this.aussteigerInbound = aussteigerInbound;
	}

	public int getEinsteigerOutboundSim() {
		return einsteigerOutboundSim;
	}

	public void setEinsteigerOutboundSim() {
		this.einsteigerOutboundSim += 1;
	}

	public int getAussteigerOutboundSim() {
		return aussteigerOutboundSim;
	}

	public void setAussteigerOutboundSim() {
		this.aussteigerOutboundSim += 1;
	}

	public int getEinsteigerInboundSim() {
		return einsteigerInboundSim;
	}

	public void setEinsteigerInboundSim() {
		this.einsteigerInboundSim += 1;
	}

	public int getAussteigerInboundSim() {
		return aussteigerInboundSim;
	}

	public void setAussteigerInboundSim() {
		this.aussteigerInboundSim += 1;
	}
}
