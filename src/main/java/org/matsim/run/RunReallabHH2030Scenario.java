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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

import java.io.IOException;

//TODO provide some information on the scenario
public class RunReallabHH2030Scenario {

	private static final Logger log = Logger.getLogger(RunReallabHH2030Scenario.class);

	//TODO: incorporate 1) mobility budget, 2) increased bike attractiveness !!!
	public static void main(String[] args) throws IOException {

		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			args = new String[] {"scenarios/input/hamburg-v2.0-10pct.config.drtFeederInHH.xml"};
		}

		//create config and configure drt
		Config config = RunDRTHamburgScenario.prepareConfig(args);
		//configure bike and car sharing services
		RunSharingScenario.configureBikeAndCarSharingServices(config);

		{//TODO bfre release: delete!!
			//set runId and output directory
			config.controler().setRunId("hamburg-v2.0-10pct-reallab2030");
			config.controler().setOutputDirectory("scenarios/output/output-hamburg-v2.0-10pct-reallabHH2030");

			//set real (1pct) input plans
			config.plans().setInputFile("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v1/hamburg-v1.1/input/hamburg-v1.1-1pct.plans.xml.gz");
		}

		//load the scenario. Prepare the network and the transit schedule for drt operation.
		Scenario scenario = RunDRTHamburgScenario.prepareScenario(config);
		//now add the sharing modes to the network
		RunSharingScenario.addSharingModesToNetwork(scenario.getNetwork());

		//instantiate controler. Load all drt-related modules.
		Controler controler = RunDRTHamburgScenario.prepareControler(scenario);
		//add sharing modules and configure the qsim components
		RunSharingScenario.addModulesAndConfigureQSim(scenario, controler);

		//run the simulation
		controler.run();
	}

}
