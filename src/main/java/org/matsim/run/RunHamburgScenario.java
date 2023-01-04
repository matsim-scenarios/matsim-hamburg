package org.matsim.run;


import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.population.CleanPopulation;
import org.matsim.application.prepare.population.ExtractHomeCoordinates;
import org.matsim.application.prepare.population.FixSubtourModes;
import org.matsim.contrib.drt.estimator.MultiModalDrtLegEstimator;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.run.MultiModeDrtEstimatorConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.commands.StrategyOptions;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.PtTripEstimator;
import org.matsim.modechoice.pruning.DistanceBasedPruner;
import org.matsim.modechoice.pruning.ModeDistanceBasedPruner;
import picocli.CommandLine;
import playground.vsp.pt.fare.DistanceBasedPtFareParams;
import playground.vsp.pt.fare.PtFareConfigGroup;
import playground.vsp.pt.fare.PtTripFareEstimator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;


/**
 * Run class using {@link MATSimApplication}
 *
 * @author rakow
 */
@MATSimApplication.Prepare({
		FixSubtourModes.class, CleanPopulation.class, ExtractHomeCoordinates.class
})
@CommandLine.Command(header = ":: Open Hamburg Scenario ::", version = RunBaseCaseHamburgScenario.VERSION)
public class RunHamburgScenario extends MATSimApplication {

	private static final Logger log = LogManager.getLogger(RunHamburgScenario.class);

	@CommandLine.Mixin
	private StrategyOptions strategy = new StrategyOptions(StrategyOptions.ModeChoice.subTourModeChoice, "person");

	@CommandLine.Option(names = "--with-drt", defaultValue = "false", description = "enable DRT functionality. you need to provide the corresponding config groups...")
	private boolean drt;

	public static void main(String[] args) {
		MATSimApplication.run(RunHamburgScenario.class, args);
	}

	public RunHamburgScenario() {
		super("scenarios/input/hamburg-v4.0-1pct.config.drt.xml");
	}

	public RunHamburgScenario(@Nullable Config config) {
		super(config);
	}

	@Override
	protected Config prepareConfig(Config config) {

		RunBaseCaseHamburgScenario.prepareConfig(config);

		{ //informed mode choice stuff. part of this potentially should be migrated to RunDRTHamburgScenario
			strategy.applyConfig(config, this::addRunOption);

			if(drt){
//				ConfigGroup[] customModulesToAdd = new ConfigGroup[] { new DvrpConfigGroup(), new MultiModeDrtConfigGroup(),
//						new SwissRailRaptorConfigGroup(), new IntermodalTripFareCompensatorsConfigGroup(),
//						new PtIntermodalRoutingModesConfigGroup()};

				//materialize config groups
				MultiModeDrtConfigGroup mmDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
				ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
				ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);
//				ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);


				// Use estimators with default values
				MultiModeDrtEstimatorConfigGroup estimatorConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtEstimatorConfigGroup.class);
				for (DrtConfigGroup drtCfg : mmDrtCfg.getModalElements()) {
					estimatorConfig.addParameterSet(new DrtEstimatorConfigGroup(drtCfg.getMode()));
				}

				//when simulating dvrp, we need/should simulate from the start to the end
				config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
				config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

				DrtConfigs.adjustMultiModeDrtConfig(mmDrtCfg, config.planCalcScore(), config.plansCalcRoute());
			}

		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		try {
			RunBaseCaseHamburgScenario.prepareScenario(scenario);
			if(drt){
				RunDRTHamburgScenario.prepareNetwork(scenario);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	@Override
	protected void prepareControler(Controler controler) {

		RunBaseCaseHamburgScenario.prepareControler(controler);
		if(drt){
			RunDRTHamburgScenario.prepareControler(controler);
		}
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if(drt){
					install(new DrtEstimatorModule());
				}
				// Configure informed mode choice strategy
				install(strategy.applyModule(binder(), controler.getConfig(), builder ->{
						builder.withFixedCosts(FixedCostsEstimator.DailyConstant.class, TransportMode.car, TransportMode.pt)
										.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.bike, TransportMode.ride, TransportMode.walk)
										.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, TransportMode.car)
										.withTripEstimator(PtTripEstimator.class, ModeOptions.AlwaysAvailable.class, TransportMode.pt)
										.withActivityEstimator(DefaultActivityEstimator.class)
										// These are with activity estimation enabled
										.withPruner("ad999", new DistanceBasedPruner(3.03073657, 0.22950583))
										.withPruner("ad99", new DistanceBasedPruner(2.10630819, 0.0917091))
										.withPruner("ad95", new DistanceBasedPruner(1.72092386, 0.03189323));
						if(drt) {
							builder.withLegEstimator(MultiModalDrtLegEstimator.class, ModeOptions.AlwaysAvailable.class, "drt");
						}
					})
				);
			}
		});

	}
}
