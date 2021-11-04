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

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.contrib.dvrp.run.MultiModals;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModes;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.PreplanningEngineQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

//TODO provide some information on the scenario
public class RunReallabHH2030Scenario {

	private static final Logger log = Logger.getLogger(RunReallabHH2030Scenario.class);

	public static void main(String[] args) throws IOException {

		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.2/input/reallab2030/hamburg-v2.2-10pct.config.reallabHH2030.xml"};
		}

		Config config = prepareConfig(args);
		Scenario scenario = prepareScenario(config);

		Controler controler = prepareControler(scenario);

		//run the simulation
		controler.run();
	}

	public static Config prepareConfig(String[] args) {
		//for the config, we need to call RunDRTHamburgScenario before RunSharingScenario because it also loads all config groups needed.
		//create config and configure drt
		Config config = RunDRTHamburgScenario.prepareConfig(args);

		//important: do this first, as later, the modeParams for bike are copied into modeParams for bike sharing!
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

		//instantiate controler. add sharing modules and configure the qsim components (we have to reconfigure the latter later again, see below. otherwise they get overwritten by drt)
		Controler controler = RunSharingScenario.prepareControler(scenario);

		//Load all drt-related modules and configure the drt qsim components.
		RunDRTHamburgScenario.prepareControler(controler);

		//the qsim components of sharing and drt overwrote each other. so we have to reconfigure everything jointly.
		MultiModeDrtConfigGroup drtfg = MultiModeDrtConfigGroup.get(controler.getConfig());
		SharingConfigGroup sharingConfig = ConfigUtils.addOrGetModule(controler.getConfig(), SharingConfigGroup.class);
		controler.configureQSimComponents(QSimComponentConfigurator(sharingConfig, drtfg.getModalElements().stream().map(Modal::getMode).collect(toList())));

		//add mobility budget (monetary incentive to abandon car) in €/day. this is available for persons that had used car in the input plans, only.
		Double mobilityBudget = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).getfixedDailyMobilityBudget();
		Preconditions.checkNotNull(mobilityBudget, "you need to specify fixedDailyMobilityBudget in " + HamburgExperimentalConfigGroup.class);
		Map<Id<Person>, Double> person2MobilityBudget = RunBaseCaseWithMobilityBudget.getPersonsEligibleForMobilityBudget2FixedValue(scenario, mobilityBudget);
		MobilityBudgetEventHandler mobilityBudgetHandler = new MobilityBudgetEventHandler(person2MobilityBudget);
		RunBaseCaseWithMobilityBudget.addMobilityBudgetHandler(controler, mobilityBudgetHandler);
		return controler;
	}

	private static QSimComponentsConfigurator QSimComponentConfigurator(SharingConfigGroup sharingConfig, List<String> dvrpModes){
		return components -> {
			for (SharingServiceConfigGroup serviceConfig : sharingConfig.getServices()) {
				components.addComponent(SharingModes.mode(SharingUtils.getServiceMode(serviceConfig)));
			}

			components.addNamedComponent(DynActivityEngine.COMPONENT_NAME);
			components.addNamedComponent(PreplanningEngineQSimModule.COMPONENT_NAME);

			//activate all DvrpMode components
			MultiModals.requireAllModesUnique(dvrpModes);
			for (String m : dvrpModes) {
				components.addComponent(DvrpModes.mode(m));
			}
		};
	}

}
