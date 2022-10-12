package org.matsim.prepare.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;
import java.util.Set;

public class RemoveBridge {

	private static final Set<String> modesToClean = Set.of("commercial_PWV_IV", "commercial_Lkw-g", "commercial_Pkw-Lfw", "bike", "commercial_Trans", "ride", "commercial_Lfw", "car", "commercial_Lkw-m", "commercial_Lkw-k");
	private static final Set<Id<Link>> linksToRemove = Set.of(
			Id.createLinkId("2930445760006f"),Id.createLinkId("1461817310002f"),
			Id.createLinkId("338176290009f"),Id.createLinkId("338176300003f"),
			Id.createLinkId("581368510009f"), Id.createLinkId("105892220000f")
			);

	private static final List<Id<Node>> connectNodes = List.of(Id.createNodeId("2284802759"), Id.createNodeId("721423355"));

	public static void main(String[] args) {

		var network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-network-with-pt.xml.gz", ConfigUtils.createConfig());

		for (var id : linksToRemove) {
			network.removeLink(Id.createLinkId(id));
		}

		for (var mode : modesToClean) {
			new MultimodalNetworkCleaner(network).run(Set.of(mode));
		}

		NetworkUtils.writeNetwork(network, "./scenarios/input/hamburg-v3.0-1pct-removed-streets.network.xml.gz");

		var population = PopulationUtils.readPopulation("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-1pct-base.plans.xml.gz");

		for (var person : population.getPersons().values()) {

			for (var plan : person.getPlans()) {
				for (var leg : TripStructureUtils.getLegs(plan)) {

					var route = leg.getRoute();
					if (route instanceof NetworkRoute) {
						var networkRoute = (NetworkRoute)route;
						// invalidate all routes which have links that are no longer in the network
						if (networkRoute.getLinkIds().stream().anyMatch(id -> !network.getLinks().containsKey(id))) {
							leg.setRoute(null);
						}
					}
				}

				for (var act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.StagesAsNormalActivities)) {
					if (!network.getLinks().containsKey(act.getLinkId())) {
						act.setLinkId(null);
					}
				}
			}
		}

		PopulationUtils.writePopulation(population, "./scenarios/input/hamburg-v3.0-1pct-removed-streets.population.xml.gz");
	}
}