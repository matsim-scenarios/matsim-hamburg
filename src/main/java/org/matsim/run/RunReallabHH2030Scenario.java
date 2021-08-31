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



	private static void adjustBikeParameters(Config config) {
		//TODO: also adjust bike sharing mode params!!!
		PlanCalcScoreConfigGroup.ModeParams bikeParams = config.planCalcScore().getModes().get(TransportMode.bike);

		double ce_beta_lane = 1.08; //utility increase of a bike lane over no bike infrastructure
		double ce_beta_time = -0.258; // in utils/minute
		double ce_timeUtility_min_lane = ce_beta_lane / ce_beta_time * -1;

		double mtsm_total_time_costs_min = (bikeParams.getMarginalUtilityOfTraveling() - config.planCalcScore().getPerforming_utils_hr()) / 60.;
		double mtsm_utility_lane = mtsm_total_time_costs_min * ce_timeUtility_min_lane;

		bikeParams.setConstant(bikeParams.getConstant() + mtsm_utility_lane);


		//the stuff underneath is old! TODO delete
//		VERSION 2 => wrong, because the underlying choice experiment does not really allow for a relation of beta_lane and avg_tt!
//
//		//		prefix ce is for 'choiceExperiment'
//		double ce_avg_tt = 11.2; //11.2 minutes
//		double ce_beta_lane = 1.08; //utility increase of a bike lane over no bike infrastructure
//		double ce_beta_time = -0.258; // in utils/minute
//		double ce_beta_time_adj = ce_beta_time + (ce_beta_lane / ce_avg_tt);
//
//		//mtsm prefix is for MATSim
//		double mtsm_total_time_costs_min = (bikeParams.getMarginalUtilityOfTraveling() - config.planCalcScore().getPerforming_utils_hr()) / 60.;
//
//		//now say that ce_beta_time_adj/ce_beta_time = mtsm_total_time_costs_min_adj = mtsm_total_time_costs_min_adj
//		double mtsm_total_time_costs_min_adj = ce_beta_time_adj / ce_beta_time * mtsm_total_time_costs_min;
//		double mtsm_beta_time_bike_adj_hr = mtsm_total_time_costs_min_adj * 60. + config.planCalcScore().getPerforming_utils_hr();
//
//		bikeParams.setMarginalUtilityOfTraveling(mtsm_beta_time_bike_adj_hr);

//		VERSION 1 => definitely wrong!

		/** according to investigations of the DLR, daily bike users experience a utility increase equal to 4.2 minutes riding time when
		 * switching from no bike infrastructure to bike lanes (Radfahrstreifen). For switching (from nothing) to protected bike paths, the utility gain is 8.5 minutes (roughly double);
		 * for bike paths (Radweg) it is 5 minutes.
		 * Currently, we have ASC_bike = 0. Moreover, it makes sense not to model this via ASC, as i doubt that persons who travel 1 minute on a street w/o
		 * bike infrastructure would rather travel 6 minutes on a bike path, but this is rather to be modeled as time sensitivity. Thus, we rather adjust
		 * the marginalUtilityOfTravelling_hr.
		 * Finally, we assume in this scenario, that bicycle infrastructure is broadly improved. Where there was no infrastructure, a bike lane is installed;
		 * where there was a bike lane, a protected bike path is installed. Thus, we reduce the time sensitivity in the equivalent of 4.2 minutes.
		 * In other words, people accept 7% longer bike rides.
		 * tschlenther aug '21
		 */
//		double oldValue = bikeParams.getMarginalUtilityOfTraveling();
//		Preconditions.checkArgument(oldValue < 0);
//		double newValue = oldValue - (oldValue / 60) * 4.2;
//		bikeParams.setMarginalUtilityOfTraveling(newValue);
	}

	public static Config prepareConfig(String[] args) {
		//for the config, we need to call RunDRTHamburgScenario before RunSharingScenario because it also loads all config groups needed.
		//create config and configure drt
		Config config = RunDRTHamburgScenario.prepareConfig(args);
		//configure bike and car sharing services
		RunSharingScenario.configureBikeAndCarSharingServices(config);

		adjustBikeParameters(config);
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

		//add mobility budget (monetary incentive to abandon car) of 5 €/day. this is available for persons that had used car in the input plans, only.
		Map<Id<Person>, Double> person2MobilityBudget = RunBaseCaseWithMobilityBudget.getPersonsEligibleForMobilityBudget2FixedValue(scenario, 5.0);
		MobilityBudgetEventHandler mobilityBudgetHandler = new MobilityBudgetEventHandler(person2MobilityBudget);
		RunBaseCaseWithMobilityBudget.addMobilityBudgetHandler(controler, mobilityBudgetHandler);
		return controler;
	}

}
