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

package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

public class CutNetwork2LinkIds {

	public static void main(String[] args) {

		String area = "hvvArea"; //set to either 'hvvArea' or 'hhCity'
		String linkId_path = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-links-in-" + area + ".tsv";
		String inputNetwork_path = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-with-pt.xml.gz";


		Set<String> linksInside;
		try {
			linksInside = LinksInShp.readLinkIdStrings(linkId_path);

			Network network = NetworkUtils.readTimeInvariantNetwork(inputNetwork_path);

			Set<Id<Link>> toDelete = network.getLinks().keySet().stream()
					.filter(linkId -> !linksInside.contains(linkId.toString()))
					.collect(Collectors.toSet());

			for (Id<Link> linkId : toDelete) {
				network.removeLink(linkId);
			}
			String outputFilePath = inputNetwork_path.substring(0, inputNetwork_path.lastIndexOf(".") -4 ) + "-" + area + ".xml.gz";
			NetworkUtils.writeNetwork(network, outputFilePath);
		} catch (FileNotFoundException e) {

		}
	}
}
