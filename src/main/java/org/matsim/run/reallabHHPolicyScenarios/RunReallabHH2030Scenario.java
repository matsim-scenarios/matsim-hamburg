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

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.dvrp.run.MultiModals;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModes;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.PreplanningEngineQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.run.HamburgExperimentalConfigGroup;
import org.matsim.run.RunDRTHamburgScenario;
import org.matsim.run.RunSharingScenario;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/** this class is used to simulate 2 policy scenarios for the year 2030: <br> <ul>
 * <li>ReallabHH2030 scenario which consists of the following measurements</i> <br> <ul>
 * <li>DRT-Feeder to public transport - only allowed for intermodal trips - no point2point service <br>
 * <li>Sharing car and sharing bike are introduced and operate with freeflowing AND are stationed at HVV switch points <br>
 * <li>heavy and wide bike infrastructure improvement. this is modeled via ASC, based on stated preference data by DLR <br>
 * <li>mobility budget: 2.5 €/day as incentive to abandon private cars <br>
 * </ul>
 * <li> Reallab2030HH plus scenario which adds a modified transit schedule to the above described scenario. This can be incorporated by the config. This is why, we use the same run class.
 * </ul>
 */
public class RunReallabHH2030Scenario {

	private static final Logger log = LogManager.getLogger(RunReallabHH2030Scenario.class);

	static final String CFG_REALLABHH2030_SCENARIO = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/reallab2030/hamburg-v3.0-10pct.config.reallabHH2030.xml";
	static final String CFG_REALLABHH2030_PLUS_SCENARIO = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/reallab2030plus/hamburg-v3.0-10pct.config.reallabHH2030-plus.xml";


	public static void main(String[] args) throws IOException {

		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			args = new String[] {CFG_REALLABHH2030_SCENARIO};
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
