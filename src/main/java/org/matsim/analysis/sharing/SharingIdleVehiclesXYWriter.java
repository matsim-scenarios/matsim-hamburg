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

package org.matsim.analysis.sharing;

import com.opencsv.CSVWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.service.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunSharingScenario;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharingIdleVehiclesXYWriter implements SharingPickupEventHandler, SharingDropoffEventHandler, SharingVehicleEventHandler, IterationEndsListener {

	private final Map<Id<Link>, Integer> nrOfVehiclesOnLink= new HashMap();
	private final String service;
	private final Network network;
	private final List<String[]> entries = new ArrayList<>();


	public static void main(String[] args) {

		String eventsFile = "D:/ReallabHH/runs/reallabHH2030/hamburg-v2.0-10pct-reallab2030.output_events.xml.gz";
		String networkFile = "D:/ReallabHH/runs/reallabHH2030/hamburg-v2.0-10pct-reallab2030.output_network.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkUtils.readNetwork(scenario.getNetwork(), networkFile);

		EventsManager manager= EventsUtils.createEventsManager();
		SharingIdleVehiclesXYWriter carHandler = new SharingIdleVehiclesXYWriter(RunSharingScenario.SHARING_SERVICE_ID_CAR, scenario.getNetwork());
		manager.addHandler(carHandler);
		SharingIdleVehiclesXYWriter bikeHandler = new SharingIdleVehiclesXYWriter(RunSharingScenario.SHARING_SERVICE_ID_BIKE, scenario.getNetwork());
		manager.addHandler(bikeHandler);

		SharingEventsReader.create(manager).readFile(eventsFile);

		carHandler.writeOutput("D:/ReallabHH/runs/reallabHH2030/hamburg-v2.0-10pct-reallab2030.car_idleVehiclesXY.csv");
		bikeHandler.writeOutput("D:/ReallabHH/runs/reallabHH2030/hamburg-v2.0-10pct-reallab2030.bike_idleVehiclesXY.csv");

	}

	public SharingIdleVehiclesXYWriter(String sharingServiceIdString, Network network) {
		service = sharingServiceIdString;
		this.network = network;
	}

	@Override
	public void handleEvent(SharingDropoffEvent event) {
		if(this.service.equals(event.getServiceId().toString())){
			increment(event.getLinkId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(SharingPickupEvent event) {
		if(this.service.equals(event.getServiceId().toString())){
			this.decrement(event.getLinkId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(SharingVehicleEvent event) {
		if(this.service.equals(event.getServiceId().toString())) {
			this.increment(event.getLinkId(), event.getTime());
		}
	}

	private void increment(Id<Link> linkId, double time){
		this.nrOfVehiclesOnLink.compute(linkId, (k,v) -> v==null? 1 : v + 1);
		recordEntry(linkId, time);
	}

	private void decrement(Id<Link> linkId, double time){
		if(!this.nrOfVehiclesOnLink.containsKey(linkId) || this.nrOfVehiclesOnLink.get(linkId) == 0) {
			throw new RuntimeException("no vehicle registered on link " + linkId);
		}
		this.nrOfVehiclesOnLink.compute(linkId, (k,v) -> v - 1);
		this.recordEntry(linkId, time);
	}

	@Override
	public void reset(int iteration) {
		SharingPickupEventHandler.super.reset(iteration);
		this.nrOfVehiclesOnLink.clear();
		this.entries.clear();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), service + "_idleVehiclesXY.csv");
		writeOutput(fileName);
	}

	private void writeOutput(String fileName) {
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(fileName));
			writer.writeNext(getHeader());
			for (String[] entry : this.entries) {
				writer.writeNext(entry);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final void recordEntry(Id<Link> linkId, double time){
		Coord coord = network.getLinks().get(linkId).getCoord();
		this.entries.add(new String[]{linkId.toString(),
				String.valueOf(coord.getX()),
				String.valueOf(coord.getY()),
				String.valueOf(time),
				String.valueOf(this.nrOfVehiclesOnLink.get(linkId)) });
	}

	private final String[] getHeader(){
		return new String[]{"linkId", "X", "Y", "time", "idleVehicles"};
	}
}
