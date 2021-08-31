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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;

import java.io.IOException;
import java.util.Map;

//TODO provide some information on the scenario
public class RunReallabHH2030Scenario {

	private static final Logger log = Logger.getLogger(RunReallabHH2030Scenario.class);

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

		//important: do this first as later, the modeParams for bike are copied into modeParams for bike sharing!
		adjustBikeParameters(config);

		//configure bike and car sharing services
		RunSharingScenario.configureBikeAndCarSharingServices(config);

		return config;
	}

	private static void adjustBikeParameters(Config config) {
		PlanCalcScoreConfigGroup.ModeParams bikeParams = config.planCalcScore().getModes().get(TransportMode.bike);

		double ce_beta_lane = 1.08; //utility increase of a bike lane over no bike infrastructure
		double ce_beta_time = -0.258; // in utils/minute
		double ce_timeUtility_min_lane = ce_beta_lane / ce_beta_time * -1;

		double mtsm_total_time_costs_min = (bikeParams.getMarginalUtilityOfTraveling() - config.planCalcScore().getPerforming_utils_hr()) / 60.;
		double mtsm_utility_lane = -mtsm_total_time_costs_min * ce_timeUtility_min_lane;

		bikeParams.setConstant(bikeParams.getConstant() + mtsm_utility_lane);
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

		//add mobility budget (monetary incentive to abandon car) of 5 €/day. this is available for persons that had used car in the input plans, only.
		Map<Id<Person>, Double> person2MobilityBudget = RunBaseCaseWithMobilityBudget.getPersonsEligibleForMobilityBudget2FixedValue(scenario, 5.0);
		MobilityBudgetEventHandler mobilityBudgetHandler = new MobilityBudgetEventHandler(person2MobilityBudget);
		RunBaseCaseWithMobilityBudget.addMobilityBudgetHandler(controler, mobilityBudgetHandler);
		return controler;
	}

}
