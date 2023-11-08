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

package org.matsim.run;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecificationImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationWithMatsimVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationWithMatsimVehicle.*;

public class RunEVScenario {

	private static final Logger log = Logger.getLogger(RunEVScenario.class);

	public static void main(String[] args) throws IOException {
		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			args = new String[] {"scenarios/input/hamburg-v3.0-1pct.config.baseCase.xml",
					"--config:controler.lastIteration", "0",
					"--config:controler.outputDirectory", "output/evTest"};
		}

		RunEVScenario evScenario = new RunEVScenario();
		evScenario.run(args);
	}


	private void run(String[] args) throws IOException {

		Config config = prepareConfig(args);

		Scenario scenario = prepareScenario(config);

		org.matsim.core.controler.Controler controler = prepareControler(scenario);

		controler.run();
		log.info("Done.");
	}

	private Scenario prepareScenario(Config config) throws IOException {
		Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

		for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
			VehicleUtils.setHbefaTechnology(vehicle.getType().getEngineInformation(), "electricity");
		}

		return scenario;
	}

	private Config prepareConfig(String[] args) {
		EvConfigGroup evCfg = new EvConfigGroup();
		evCfg.setChargersFile("ev/chargers.xml");
		evCfg.setTimeProfiles(true);
		return RunBaseCaseHamburgScenario.prepareConfig(args, evCfg);
	}

	public static org.matsim.core.controler.Controler prepareControler(Scenario scenario) {
		Controler controler =  RunBaseCaseHamburgScenario.prepareControler(scenario);

		//later, we should write our own ElectricFleet module as we will also use multiple energy consumption models etc.
		controler.addOverridingModule(new EvModule());

		//the following provider would be obsolete if we would use QSimConfigGroup.VehiclesSource.fromVehiclesData
		//i.e. if we would specify each and every vehicle in the input file individually. the provider is basically copied from ElectricVehicleSpecificationWithMatsimVehicle
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ElectricFleetSpecification.class).toProvider(new Provider<>() {
					@Inject
					private Vehicles vehicles;

					@Override
					public ElectricFleetSpecification get() {
						ElectricFleetSpecification fleetSpecification = new ElectricFleetSpecificationImpl();
						vehicles.getVehicles()
								.values()
								.stream()
								.filter(v -> v.getType().getId().toString().equals("car"))
								.forEach(v -> {
									Attributes attributes = v.getAttributes();
									EngineInformation engineInfo = v.getType().getEngineInformation();
									attributes.putAttribute(INITIAL_ENERGY_kWh, "100");
									VehicleUtils.setHbefaTechnology(engineInfo, EV_ENGINE_HBEFA_TECHNOLOGY);
									attributes.putAttribute(CHARGER_TYPES, List.of("blue"));
									VehicleUtils.setEnergyCapacity(engineInfo, 100);
									fleetSpecification.addVehicleSpecification(new ElectricVehicleSpecificationWithMatsimVehicle (v));
								});
						return fleetSpecification;
					}
				}).asEagerSingleton();
			}
		});

		return controler;
	}

}
