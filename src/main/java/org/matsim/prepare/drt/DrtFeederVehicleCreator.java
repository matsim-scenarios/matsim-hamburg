/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.prepare.drt;

import com.opencsv.CSVWriter;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.matsim.run.RunDRTHamburgScenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author  gleich, tschlenther
 *
 */
public class DrtFeederVehicleCreator {
	private static final Logger log = Logger.getLogger(DrtFeederVehicleCreator.class);

	//NOTE: for emission analysis, it is important to include 'drt' in the vehicleId
	public final static String VEHICLE_PREFIX = "drtFeeder_";

	private final CoordinateTransformation ct;
	private final Scenario scenario ;
	private final Random random = MatsimRandom.getRandom();
	private final String drtNetworkMode = RunDRTHamburgScenario.DRT_FEEDER_MODE;
	private final HamburgShpUtils shpUtils;
	private final Network drtNetwork;
	private List<Pair<Id<Link>, Double>> links2weights = new ArrayList();

	public static void main(String[] args) {


		//careful: currently reallabHH2030plus network!
		String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt-and-parkingPressure.xml.gz";
//		String populationFile = "";
//		String facilitiesFile = "";

		//where people can be picked up and dropped off

		//careful: currently drt2030 area!
		String drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030/input/drt/allDrtNetworkOperationArea/allDrtNetworkOperationArea.shp";

		//where vehicles are allowed to drive
		String drtOperationArea = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030/input/drt/allDrtNetworkOperationArea/allDrtNetworkOperationArea.shp";

		//transforms from service area crs to the network crs
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:25832", RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);

		//TODO you need to adjust this to your local copy because you can not write from your IDE.
		String vehiclesFilePrefix = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.2/input/drt2030/drt/vehicles/hamburg-v2.2-drt-by-rndLocations-";

		Set<Integer> numbersOfVehicles = new HashSet<>();
		numbersOfVehicles.add(500);
		numbersOfVehicles.add(600);
		numbersOfVehicles.add(700);
		numbersOfVehicles.add(800);
		numbersOfVehicles.add(900);
		numbersOfVehicles.add(1000);
//		numbersOfVehicles.add(1200);
		numbersOfVehicles.add(1500);
		numbersOfVehicles.add(2000);
//		numbersOfVehicles.add(2500);
		numbersOfVehicles.add(3000);
		numbersOfVehicles.add(4000);
		numbersOfVehicles.add(5000);
		numbersOfVehicles.add(10000);
		int seats = 8;

		DrtFeederVehicleCreator tvc = new DrtFeederVehicleCreator(networkFile, drtServiceAreaShapeFile, drtOperationArea, ct);
//		tvc.setLinkWeightsByActivities(populationFile, facilitiesFile);
//		tvc.setWeightsToSquareRoot();
		for (int numberOfVehicles: numbersOfVehicles) {
//			tvc.createVehiclesByWeightedDraw(numberOfVehicles, seats, vehiclesFilePrefix);
			tvc.createVehiclesByRandomPointInShape(numberOfVehicles, seats, vehiclesFilePrefix);
		}
}

	public DrtFeederVehicleCreator(String networkfile, String drtServiceAreaShapeFile, String drtOperationArea, CoordinateTransformation ct) {
		this.ct = ct;
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputCRS(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
		config.global().setCoordinateSystem(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
		config.network().setInputFile(networkfile);
		this.scenario = ScenarioUtils.loadScenario(config);

		shpUtils = new HamburgShpUtils(drtServiceAreaShapeFile);

		//prepare network
		RunDRTHamburgScenario.addDRTmode(scenario, drtNetworkMode, drtOperationArea, 0);
		Set<String> modes = new HashSet<>();
		modes.add(drtNetworkMode);
		drtNetwork = NetworkUtils.createNetwork();
		Set<String> filterTransportModes = new HashSet<>();
		filterTransportModes.add(drtNetworkMode);
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(drtNetwork, filterTransportModes);

		new NetworkWriter(drtNetwork).write("drtNetwork.xml.gz");

	}

	public final void createVehiclesByWeightedDraw(int amount, int seats, String vehiclesFilePrefix) {
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		EnumeratedDistribution<Id<Link>> weightedLinkDraw = new EnumeratedDistribution<>(links2weights);

		for (int i = 0 ; i< amount; i++) {
			Id<Link> linkId = weightedLinkDraw.sample();
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create(VEHICLE_PREFIX + i, DvrpVehicle.class))
					.startLinkId(linkId)
					.capacity(seats)
					.serviceBeginTime(Math.round(1))
					.serviceEndTime(Math.round(30 * 3600))
					.build());
		}
		String fileNameBase = vehiclesFilePrefix + amount + "vehicles-" + seats + "seats";
		new FleetWriter(vehicles.stream()).write(fileNameBase + ".xml.gz");

		writeVehStartPositionsCSV(vehicles, fileNameBase);
	}

	private void writeVehStartPositionsCSV(List<DvrpVehicleSpecification> vehicles, String fileNameBase) {
		Map<Id<Link>, Long> linkId2NrVeh = vehicles.stream().
				map(veh -> veh.getStartLinkId()).
				collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(fileNameBase + "_startPositions.csv")), ';', '"', '"', "\n");
			writer.writeNext(new String[]{"link", "x", "y", "drtVehicles"}, false);
			linkId2NrVeh.forEach( (linkId, numberVeh) -> {
				Coord coord = scenario.getNetwork().getLinks().get(linkId).getCoord();
				double x = coord.getX();
				double y = coord.getY();
				writer.writeNext(new String[]{linkId.toString(), "" + x, "" + y, "" + numberVeh}, false);
			});

			writer.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public final void setLinkWeightsByActivities(String populationFile, String facilitiesFile) {
		links2weights.clear(); // initial reset if already set before
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(populationFile); //TODO: coord transformations
		if (facilitiesFile != null && !facilitiesFile.equals("")) {
			MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
			facilitiesReader.readFile(facilitiesFile); //TODO: coord transformations
		}

		Map<Id<Link>, Long> link2Occurences = scenario.getPopulation().getPersons().values().stream().
				map(person -> person.getSelectedPlan()).
				map(plan -> plan.getPlanElements()).
				flatMap(planElements -> planElements.stream()).
				filter(planElement -> planElement instanceof Activity).
				map(planElement -> (Activity) planElement).
				filter(activity -> activity.getType().equals(TripStructureUtils.createStageActivityType(TransportMode.pt)) || !StageActivityTypeIdentifier.isStageActivity(activity.getType())).
				filter(activity -> shpUtils.isCoordInDrtServiceAreaWithBuffer(PopulationUtils.decideOnCoordForActivity(activity, scenario), 2000.0)).
				map(activity -> getLinkIdOnDrtNetwork(activity)).
				collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		for (Map.Entry<Id<Link>, Long> entry : link2Occurences.entrySet()) {
			// check link is in shape file
			Link link = scenario.getNetwork().getLinks().get(entry.getKey());

			if (shpUtils.isCoordInServiceArea(link.getFromNode().getCoord()) && shpUtils.isCoordInServiceArea(link.getToNode().getCoord())) {
				links2weights.add(new Pair<>(entry.getKey(), entry.getValue().doubleValue()));
			} // else forget that link because it's not usable for drt
		}
	}

	private Id<Link> getLinkIdOnDrtNetwork(Activity activity) {
		Id<Link> linkId = PopulationUtils.decideOnLinkIdForActivity(activity, scenario);
		if (!drtNetwork.getLinks().containsKey(linkId)) {
			linkId = NetworkUtils.getNearestLink(drtNetwork, PopulationUtils.decideOnCoordForActivity(activity, scenario)).getId();
		}
		return linkId;
	}

	/**
	 * Overwrites weights with the square root of the previous weight. Shifts some vehicles to otherwise empty areas.
	 */
	public final void setWeightsToSquareRoot() {
		links2weights = links2weights.stream().map(pair -> new Pair<>(pair.getFirst(), Math.sqrt(pair.getSecond()))).collect(Collectors.toList());
	}

	public final void createVehiclesByRandomPointInShape(int amount, int seats, String vehiclesFilePrefix) {
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

		for (int i = 0 ; i< amount; i++) {
			Link link = null;
			
			while (link == null) {
				Point p = shpUtils.getRandomPointInServiceArea(random);
				link = NetworkUtils.getNearestLinkExactly(drtNetwork, ct.transform( MGC.point2Coord(p)));
				if(link == null){
					log.warn("could not find nearest link for " + p + " (transformed: " + MGC.point2Coord(p)+ ") ");
				}
				if (shpUtils.isCoordInServiceArea(link.getFromNode().getCoord()) && shpUtils.isCoordInServiceArea(link.getToNode().getCoord())) {
					if (link.getAllowedModes().contains(drtNetworkMode)) {
						// ok
					} else {
						link = null;
					}
					// ok, the link is within the shape file
				} else {
					link = null;
				}
			}
			
			if (i%100 == 0) log.info("#"+i);

			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create(VEHICLE_PREFIX + i, DvrpVehicle.class))
					.startLinkId(link.getId())
					.capacity(seats)
					.serviceBeginTime(Math.round(1))
					.serviceEndTime(Math.round(30 * 3600))
					.build());


		}
		String fileNameBase = vehiclesFilePrefix + amount + "vehicles-" + seats + "seats";
		new FleetWriter(vehicles.stream()).write(fileNameBase + ".xml.gz");

		writeVehStartPositionsCSV(vehicles, fileNameBase);
	}

}
