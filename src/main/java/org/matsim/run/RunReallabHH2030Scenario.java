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
			//TODO change ?!
			args = new String[] {"scenarios/input/hamburg-v2.0-10pct.config.drtFeederInHH.xml"};
		}

		Config config = prepareConfig(args);

		{//TODO bfre release: delete!!
			//set runId and output directory
			config.controler().setRunId("hamburg-v2.0-10pct-reallab2030");
//			config.controler().setOutputDirectory("scenarios/output/output-hamburg-v2.0-1pct-reallabHH2030");

			//set real (1pct) input plans
//			config.plans().setInputFile("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v1/hamburg-v1.1/input/hamburg-v1.1-1pct.plans.xml.gz");
		}

		Scenario scenario = prepareScenario(config);

		Controler controler = prepareControler(scenario);

		//run the simulation
		controler.run();
	}

	public static Config prepareConfig(String[] args) {
		//for the config, we need to call RunDRTHamburgScenario before RunSharingScenario because it also loads all config groups needed.
		//create config and configure drt
		Config config = RunDRTHamburgScenario.prepareConfig(args);
		//configure bike and car sharing services
		RunSharingScenario.configureBikeAndCarSharingServices(config);
		return config;
	}

	public static Scenario prepareScenario(Config config) throws IOException {
		//load the scenario. Add the sharing modes to the network
		Scenario scenario = RunSharingScenario.prepareScenario(config);
		//now prepare the network and the transit schedule for drt operation.
		RunDRTHamburgScenario.prepareNetwork(scenario);
		return scenario;
	}

	public static Controler prepareControler(Scenario scenario) {
		//we need to configure sharing first!! Otherwise, drt will not be simulated and leads to exceptions!!

		//instantiate controler. add sharing modules and configure the qsim components
		Controler controler = RunSharingScenario.prepareControler(scenario);
		//Load all drt-related modules and configure the drt qsim components.
		RunDRTHamburgScenario.prepareControler(controler);
		return controler;
	}

}
