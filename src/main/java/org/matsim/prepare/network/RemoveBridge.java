package org.matsim.prepare.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import java.util.Set;

public class RemoveBridge {

	private static final Set<Id<Link>> linksToRemove = Set.of(Id.createLinkId("1"),Id.createLinkId("1"));

	private static void main(String[] args) {

		var network = NetworkUtils.readNetwork("", ConfigUtils.createConfig());

		for (var id : linksToRemove) {
			network.removeLink(Id.createLinkId(id));
		}

		NetworkUtils.writeNetwork(network, "");

		var population = PopulationUtils.readPopulation("");

		for (var person : population.getPersons().values()) {
			for (var plan : person.getPlans()) {
				for (var leg : TripStructureUtils.getLegs(plan)) {

					var route = leg.getRoute();
					if (route instanceof NetworkRoute) {
						var networkRoute = (NetworkRoute)route;
						if (networkRoute.getLinkIds().stream().anyMatch(linksToRemove::contains)) {
							leg.setRoute(null);
						}
					}
				}
			}
		}
	}
}