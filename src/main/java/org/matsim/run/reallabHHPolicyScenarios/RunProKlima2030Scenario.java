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

package org.matsim.run.reallabHHPolicyScenarios;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.parking.UtilityBasedParkingPressureEventHandler;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This policy case combines all measures simulated in the ReallabHH2030-plus scenario (which is ReallabHH2030 plus an improved pt schedule) with push measures on cars. <br>
 * See also {@link RunReallabHH2030Scenario}.
 */
public class RunProKlima2030Scenario {

	/**
	 * this is the area in which car links are modified. The currently used shape consists of several polygons (city districts)
	 */
	public static final String SHP_CAR_PUSH_MEASURES_AREA = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg_city/hamburg_stadtteil.shp";
	private static final Logger log = Logger.getLogger(RunProKlima2030Scenario.class);

	public static void main(String[] args) throws IOException {

		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			args = new String[] {RunReallabHH2030Scenario.CFG_REALLABHH2030_PLUS_SCENARIO,
					"--config:controler.outputDirectory", "scenarios/output/output-hamburg-v2.0-10pct-proKlima2030",
					"--config:controler.runId", "hamburg-v2.2-proKlima2030"};
		}

		Config config = prepareConfig(args);
		Scenario scenario = prepareScenario(config);

		Controler controler = prepareControler(scenario);

		//run the simulation
		controler.run();
	}

	public static Config prepareConfig(String[] args){
		return RunReallabHH2030Scenario.prepareConfig(args);
	}

	public static Scenario prepareScenario(Config config) throws IOException {
		Scenario scenario = RunReallabHH2030Scenario.prepareScenario(config);
		implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(SHP_CAR_PUSH_MEASURES_AREA)));
		return scenario;
	}

	public static Controler prepareControler(Scenario scenario){
		return RunReallabHH2030Scenario.prepareControler(scenario);
	}

	/**
	 * Currently implemented push measures: <br>
	 * 1. Set all car links but primary roads and motorways in {@code geometries} to 30 km/h <br>
	 * 2. reduce number of lanes on primary roads by 1<br>
	 * 3. increase parking pressure on all car links by 0.7 <br>
	 *
	 * @param network
	 * @param geometries
	 */
	private static void implementPushMeasuresByModifyingNetworkInArea(Network network, List<PreparedGeometry> geometries) {
		Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
				.filter(link -> link.getAllowedModes().contains(TransportMode.car)) //filter car links
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries)) //spatial filter
				.filter(link -> ! ((String)link.getAttributes().getAttribute("type")).contains("motorway") ) //we won't change motorways and motorway_links
				.collect(Collectors.toSet());


		carLinksInArea.forEach(link -> {
			//increase parking pressure utility decrease by 0.7
			double oldParkingPressure = (double) link.getAttributes().getAttribute(UtilityBasedParkingPressureEventHandler.PARK_PRESSURE_ATTRIBUTE_NAME);
			link.getAttributes().putAttribute(UtilityBasedParkingPressureEventHandler.PARK_PRESSURE_ATTRIBUTE_NAME, oldParkingPressure - 0.7);

			if(link.getAttributes().getAttribute("type").equals("primary")){
				double oldCapacity = link.getCapacity();
				double oldLanes = link.getNumberOfLanes();
				if(oldLanes >= 2.0){
					link.setNumberOfLanes(oldLanes - 1); //reduce lanes
					link.setCapacity(link.getNumberOfLanes() * (oldCapacity / oldLanes)); //reduce capacity accordingly
				}
			} else {
				//apply 'tempo 30' to all roads but primary and motorways
				if(link.getFreespeed() > 7.5) link.setFreespeed(7.5); //27 km/h is used in the net for 30 km/h streets
			}
		});

	}


}
