package org.matsim.prepare.network;

import com.sun.jdi.connect.Transport;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.HashSet;
import java.util.Set;

public class AddCarFreeZone {

	public static void main(String[] args) {

		var network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-network-with-pt.xml.gz", ConfigUtils.createConfig());
		var geometry = ShapeFileReader.getAllFeatures("C:\\Users\\Janekdererste\\Desktop\\test-shape-2.shp").stream()
				.map(simpleFeature -> (Geometry)simpleFeature.getDefaultGeometry())
				.findFirst()
				.orElseThrow();

		Set<Id<Link>> linksToRemove = new HashSet<>();
		for (Link link : network.getLinks().values()) {

			var fromPoint = MGC.coord2Point(link.getFromNode().getCoord());
			var toPoint = MGC.coord2Point(link.getToNode().getCoord());

			if (geometry.contains(fromPoint) || geometry.contains(toPoint) || link.getId().equals(Id.createLinkId("354086491"))) {

				var allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.remove(TransportMode.car);
				link.setAllowedModes(allowedModes);
				linksToRemove.add(link.getId());
			}
		}

		new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));

		NetworkUtils.writeNetwork(network, "./scenarios/input/hamburg-v3.0-1pct-car-free-zone.network.xml.gz");

		var population = PopulationUtils.readPopulation("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-1pct-base.plans.xml.gz");

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
				for (var act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
					if (linksToRemove.contains(act.getLinkId())) {
						act.setLinkId(null);
					}
				}
			}
		}

		PopulationUtils.writePopulation(population, "./scenarios/input/hamburg-v3.0-1pct-car-free-zone.population.xml.gz");
	}
}