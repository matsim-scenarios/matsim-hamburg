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

package org.matsim.prepare.population;

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetLinkIdsToCarNetwork {

	public static void main(String[] args) {

		String inputNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-network-with-pt.xml.gz";
		String outputNetwork = "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-car-network.xml.gz";
		String inputPopulation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-10pct-base.plans.xml.gz";
		String outputPopulation = "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-10pct-base.plans-acts-on-car-network.xml.gz";

		Network net = NetworkUtils.readTimeInvariantNetwork(outputNetwork);
//		filterAndWriteCarNetwork(net, outputNetwork);

		//read population
		Population population = PopulationUtils.readPopulation(inputPopulation);
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<Activity> acts = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
				for (Activity act : acts) {
					if(! net.getLinks().containsKey(act.getLinkId())){ //if linkId is not contained in car network
						Link link = NetworkUtils.getNearestLink(net, act.getCoord());
						Preconditions.checkArgument(link.getAllowedModes().contains(TransportMode.car));
						act.setLinkId(link.getId());
					}
				}
			}
		}

		PopulationUtils.writePopulation(population, outputPopulation);
	}

	private static void filterAndWriteCarNetwork(Network net, String outputNetwork) {
		//filter car network
		Set<Id<Link>> nonCarLinks = net.getLinks().values().stream()
				.filter(link -> !link.getAllowedModes().contains(TransportMode.car))
				.map(link -> link.getId())
				.collect(Collectors.toSet());
		nonCarLinks.forEach(nonCarLink -> net.removeLink(nonCarLink));
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(net);
		cleaner.run(Set.of(TransportMode.car));
		cleaner.removeNodesWithoutLinks();
		NetworkUtils.writeNetwork(net, outputNetwork);
	}
}
