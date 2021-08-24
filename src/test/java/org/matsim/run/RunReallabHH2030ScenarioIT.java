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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.io.IOException;

public class RunReallabHH2030ScenarioIT {

	private final static String CONFIG = "test/input/test-hamburg.reallab2030HH.config.xml";

	@Test
	public void testRunReallabHH2030Scenario(){
		Exception exception = null;
		try {
			Config config = RunReallabHH2030Scenario.prepareConfig(new String[]{CONFIG});

			//TODO: avoid this configuration?!
			SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config,SharingConfigGroup.class);
			for (SharingServiceConfigGroup service : sharingConfigGroup.getServices()) {
				switch (service.getMode()){
					case "sbike":
						service.setServiceInputFile("shared_bike_vehicles_stations.xml");
						break;
					case "scar":
						service.setServiceInputFile("shared_car_vehicles_stations.xml");
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
			RunReallabHH2030Scenario.prepareControler(RunReallabHH2030Scenario.prepareScenario(config)).run();
		} catch (IOException e) {
			exception = e;
		}

		Assert.assertNull("An exception occured! Look into the log file!", exception);
	}
}
