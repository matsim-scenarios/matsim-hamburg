package org.matsim.run;


import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.analysis.HamburgIntermodalAnalysisModeIdentifier;
import org.matsim.analysis.PlanBasedTripsFileWriter;
import org.matsim.analysis.PlanBasedTripsWriterControlerListener;
import org.matsim.analysis.here.HereAPIControlerListener;
import org.matsim.analysis.here.HereAPITravelTimeValidation;
import org.matsim.analysis.here.HereAPITravelTimeValidationConfigGroup;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.massConservation4SingleTripModeChoice.PenalizeMassConservationViolationsModule;
import org.matsim.parking.NetworkParkPressureReader;
import org.matsim.parking.UtilityBasedParkingPressureEventHandler;
import org.matsim.prepare.freight.bvm.AdjustScenarioForFreight;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * @author zmeng, tschlenther
 */
public class RunBaseCaseHamburgScenario {

    private static final Logger log = Logger.getLogger(RunBaseCaseHamburgScenario.class);

    public static final String COORDINATE_SYSTEM = "EPSG:25832";
    public static final String VERSION = "v2.2";
    public static final double[] X_EXTENT = new double[]{490826.5738238178, 647310.6279172485};
    public static final double[] Y_EXTENT = new double[]{5866434.167201331, 5996884.970634732};

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.2/input/baseCase/hamburg-v2.2-10pct.config.baseCase.xml"};
        }

        RunBaseCaseHamburgScenario baseCaseHH = new RunBaseCaseHamburgScenario();
        baseCaseHH.run(args);
    }

    private void run(String[] args) throws IOException {

        Config config = prepareConfig(args);
        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();
        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                // use PersonIncomeSpecificScoringFunction if is needed
                if(ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).isUsePersonIncomeBasedScoring()){
                    bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);
                }
               //use custom AnalysisMainModeIdentifier
                bind(AnalysisMainModeIdentifier.class).toInstance(new HamburgIntermodalAnalysisModeIdentifier());


                //analyse PersonMoneyEvents
                install(new PersonMoneyEventsAnalysisModule());

                //if changeSingleTripMode strategy is used, install module for mass conservation scoring
                if(getConfig().strategy().getStrategySettings().stream()
                        .filter(settings -> settings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode))
                        .findAny()
                        .isPresent()){
                    install(new PenalizeMassConservationViolationsModule());
                }
            }
        });
        // use HereApiValidator if is needed
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                if(ConfigUtils.addOrGetModule(scenario.getConfig(),HereAPITravelTimeValidationConfigGroup.class).isUseHereAPI()){
                    CarTripsExtractor carTripsExtractor = new CarTripsExtractor(scenario.getPopulation().getPersons().keySet(), scenario.getNetwork());
                    this.addEventHandlerBinding().toInstance(carTripsExtractor);
                    this.addControlerListenerBinding().to(HereAPIControlerListener.class);
                    this.bind(HereAPITravelTimeValidation.class).toInstance(new HereAPITravelTimeValidation(carTripsExtractor,scenario.getConfig()));
                }
            }
        });

        // use pt validation
//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                PtValidator ptValidator = new PtValidator();
//                this.addEventHandlerBinding().toInstance(ptValidator);
//                this.addControlerListenerBinding().toInstance(new PtValidatorControlerListener(ptValidator));
//            };
//        });

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(PlanBasedTripsFileWriter.class).asEagerSingleton();
                this.addControlerListenerBinding().to(PlanBasedTripsWriterControlerListener.class);
                }
        });

        // use link-based park pressure
        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                this.addEventHandlerBinding().to(UtilityBasedParkingPressureEventHandler.class);
            }
        });

        // add Freight
        AdjustScenarioForFreight.adjustControlerForFreight(controler, AdjustScenarioForFreight.getFreightModes());

        return controler;
    }

    public static Scenario prepareScenario(Config config) throws IOException {

        /*
         * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
         * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
         * class is also used for runs without drt.
         */
        final Scenario scenario = ScenarioUtils.createScenario( config );

        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        ScenarioUtils.loadScenario(scenario);

        HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);

        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), hamburgExperimentalConfigGroup.getPopulationDownsampleFactor());

        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            person.getPlans().clear();
            person.addPlan(selectedPlan);
            person.setSelectedPlan(selectedPlan);
        }

        // increase flowspeed for links, where flowspeed lower than 25kmh
        // bc HERE validation has shown that vehicles travel faster in reality
        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (link.getFreespeed() < 25.5 / 3.6) {
                link.setFreespeed(link.getFreespeed() * hamburgExperimentalConfigGroup.getFreeSpeedFactor());
            }
        }

        // add parkPressureAttribute
        if (hamburgExperimentalConfigGroup.getParkPressureLinkAttributeFile() != null) {
            log.info("Adding missing park pressure link attributes based on provided files...");
            NetworkParkPressureReader networkParkPressureReader = new NetworkParkPressureReader(scenario.getNetwork(),hamburgExperimentalConfigGroup);
            networkParkPressureReader.addLinkParkTimeAsAttribute();
            log.info("Adding missing park pressure link attributes based on provided files... Done.");
        }


        // add Freight traffic
        if(hamburgExperimentalConfigGroup.isFilterCommercial()){
            List<Id<Person>> personIds = new LinkedList<>();
            for(Person person : scenario.getPopulation().getPersons().values()) {
                if (person.getId().toString().contains("commercial")) {
                    personIds.add(person.getId());
                }
            }
            for (Id<Person> personId: personIds) {
                scenario.getPopulation().removePerson(personId);
            }
        } else {
            AdjustScenarioForFreight.adjustScenarioForFreight(scenario, AdjustScenarioForFreight.getFreightModes());
        }


        if (hamburgExperimentalConfigGroup.isIncreaseStorageCapacity()) {
            for (Link link: scenario.getNetwork().getLinks().values()) {
                double originalStorageCapacity = link.getLength() / 15 * link.getNumberOfLanes() * hamburgExperimentalConfigGroup.getSampleSize()
                        / 100.0;
                int minimumLaneRequred = (int) (1 / originalStorageCapacity + 1);
                if (originalStorageCapacity < 1) {
                    link.setNumberOfLanes(minimumLaneRequred);
                }
            }
        }

        return scenario;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {

        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }


        final Config config = ConfigUtils.loadConfig(args[0], customModulesAll);
        HamburgExperimentalConfigGroup hamburgCfg = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);
        ConfigUtils.addOrGetModule(config, HereAPITravelTimeValidationConfigGroup.class);


        // delete default modes
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);

        String[] typedArgs = Arrays.copyOfRange(args, 1, args.length);
        ConfigUtils.applyCommandline(config, typedArgs);

        //todo: think about opening and closing time, there can be some overnight activities like shopping or business...
        for (long ii = 600; ii <= 97200; ii += 600) {

            for (String act : List.of("educ_higher", "educ_tertiary", "educ_other", "home", "educ_primary", "errands", "educ_secondary", "visit", "other")) {
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii + ".0").setTypicalDuration(ii));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("educ_kiga_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(18. * 3600.));
        }

        ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class).setProbaForRandomSingleTripMode(hamburgCfg.getSubTourModeChoiceProbaForSingleTripChange());

        return config;
    }
}
