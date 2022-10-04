/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.prepare.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class CopyLinkAttributes {

	private static final Logger log = LogManager.getLogger(CopyLinkAttributes.class);

	public static void main(String[] args) {

//		Network netV1_0 = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.0/hamburg-v1.0-network-with-pt.xml.gz");
		Network netV2_0 = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-with-pt.xml.gz");

		String reallabHH2030Net = "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt-and-parkingPressure.xml.gz";

		Network netV2_0_reallabHH2030plus = NetworkUtils.readNetwork(reallabHH2030Net);

//		for (Link link : netV2_0.getLinks().values()) {
//			if(link.getAllowedModes().contains(TransportMode.car)){
//				if(netV1_0.getLinks().containsKey(link.getId())){
//					link.setNumberOfLanes(netV1_0.getLinks().get(link.getId()).getNumberOfLanes());
//				} else {
//					log.warn("can not find link " + link.getId() + " in original network!");
//				}
//			}
//		}
//		NetworkUtils.writeNetwork(netV2_0, "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.1/baseCase/input/hamburg-v2.1-network-with-pt.xml.gz");

		for (Link link : netV2_0_reallabHH2030plus.getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.car)){
				if(netV2_0.getLinks().containsKey(link.getId())){
					link.setNumberOfLanes(netV2_0.getLinks().get(link.getId()).getNumberOfLanes());
					link.setFreespeed(netV2_0.getLinks().get(link.getId()).getFreespeed());
				} else {
					log.warn("can not find link " + link.getId() + " in original network!");
				}
			}
		}

		log.warn("will overwrite " + reallabHH2030Net);
		NetworkUtils.writeNetwork(netV2_0_reallabHH2030plus, reallabHH2030Net);


		log.info("finished");
	}


}
