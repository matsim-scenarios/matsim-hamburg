/*
package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Provides;
import org.apache.log4j.Logger;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.analysis.PlanBasedTripsFileWriter;
import org.matsim.analysis.PlanBasedTripsWriterControlerListener;
import org.matsim.analysis.here.HereAPIControlerListener;
import org.matsim.analysis.here.HereAPITravelTimeValidation;
import org.matsim.analysis.here.HereAPITravelTimeValidationConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.parking.NetworkParkPressureReader;
import org.matsim.parking.VehicleHandlerForParking;
import org.matsim.prepare.freight.AdjustScenarioForFreight;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class RunBaseCaseWithMobilityBudget {



    */
/**
     * @author zmeng
     *//*


        private static final Logger log = Logger.getLogger(org.matsim.run.RunBaseCaseHamburgScenario.class);

        public static final String COORDINATE_SYSTEM = "EPSG:25832";
        public static final String VERSION = "v1.0";
        public static final int SCALE = 1;
        public static final double[] X_EXTENT = new double[]{490826.5738238178, 647310.6279172485};
        public static final double[] Y_EXTENT = new double[]{5866434.167201331, 5996884.970634732};
        public static final HashMap<Id<Person>, Double > personsWithMobilityBudget = new HashMap<>();
        public static double totalSumMobilityBudget = 0;

        public static void main(String[] args) throws ParseException, IOException {

            for (String arg : args) {
                log.info(arg);
            }

            if (args.length == 0) {
                args = new String[] {"../public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-1pct/hamburg-v1.1-1pct.config.xml"};
            }
            RunBaseCaseWithMobilityBudget baseCaseHH = new RunBaseCaseWithMobilityBudget();
            baseCaseHH.run(args);
        }

        private void run(String[] args) throws IOException {

            Config config = prepareConfig(args);
            Scenario scenario = prepareScenario(config);
            org.matsim.core.controler.Controler controler = prepareControler(scenario);
            controler.run();
            log.info("Done.");
        }


        public static Controler prepareControler(Scenario scenario) {
            Controler controler = new Controler(scenario);

            // use the sbb pt raptor router
            controler.addOverridingModule( new AbstractModule() {
                @Override
                public void install() {
                    install( new SwissRailRaptorModule() );
                }
            } );

            MobilityBudgetEventHandler mobilityBudgetEventHandler = new MobilityBudgetEventHandler();
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addEventHandlerBinding().toInstance(mobilityBudgetEventHandler);
                }
            });

            // use AnalysisMainModeIdentifier instead of RoutingModeIdentifier
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(AnalysisMainModeIdentifier.class).to(DefaultAnalysisMainModeIdentifier.class);
                }
            });
            // use PersonIncomeSpecificScoringFunction if is needed
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    if(ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).isUsePersonIncomeBasedScoring()){
                        // new approach which is maybe not so nice but should require less memory
                        this.bindScoringFunctionFactory().to(IncomeDependentPlanScoringFunctionFactory.class);
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

            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    this.bind(PlanBasedTripsFileWriter.class).asEagerSingleton();
                    this.addControlerListenerBinding().to(PlanBasedTripsWriterControlerListener.class);
                }
            });

            // use link-based park time
            if(ConfigUtils.addOrGetModule(controler.getConfig(),HamburgExperimentalConfigGroup.class).isUseLinkBasedParkPressure()){

                controler.addOverridingQSimModule(new AbstractQSimModule() {

                    protected void configureQSim() {
                    }
                    @Provides
                    QNetworkFactory provideQNetworkFactory(EventsManager eventsManager, Scenario scenario, QSim qSim) {
                        ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(eventsManager, scenario);
                        factory.setVehicleHandler(new VehicleHandlerForParking(qSim, scenario));
                        return factory;
                    }
                });
            }

            // add Freight
            AdjustScenarioForFreight.adjustControlerForFreight(controler, AdjustScenarioForFreight.getFreightModes());


            controler.addControlerListener(new AfterMobsimListener() {
                @Override
                public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
                    for (Id<Person> personId : personsWithMobilityBudget.keySet()) {
                        if (personsWithMobilityBudget.get(personId)>0) {
                            log.info("Throwing money event" + "Person_Id:" + personId);
                            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, personId, personsWithMobilityBudget.get(personId), null, null));
                            totalSumMobilityBudget = totalSumMobilityBudget + personsWithMobilityBudget.get(personId);
                        }
                    }
                }

            });
            log.info("This iteration the totalSumMobilityBudget paid to the Agents was:" + totalSumMobilityBudget);
            return controler;
        }

        public static Scenario prepareScenario(Config config) throws IOException {

            */
/*
             * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
             * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
             * class is also used for runs without drt.
             *//*

            final Scenario scenario = ScenarioUtils.createScenario( config );

            RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
            routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

            ScenarioUtils.loadScenario(scenario);

            HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);

            org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), hamburgExperimentalConfigGroup.getPopulationDownsampleFactor());

            for (Person person : scenario.getPopulation().getPersons().values()) {
                Plan selectedPlan = person.getSelectedPlan();
                person.getPlans().clear();
                person.addPlan(selectedPlan);
                person.setSelectedPlan(selectedPlan);
                person.getAttributes().putAttribute("income", Math.random()*100);
                double income = (double) person.getAttributes().getAttribute("income");
                personsWithMobilityBudget.put(person.getId(), income);
            }

            // increase flowspeed for links, where flowspeed lower than 50kmh
            for (Link link : scenario.getNetwork().getLinks().values()) {
                if (link.getFreespeed() < 25.5 / 3.6) {
                    link.setFreespeed(link.getFreespeed() * hamburgExperimentalConfigGroup.getFreeSpeedFactor());
                }
            }

            // add parkPressureAttribute
            if(hamburgExperimentalConfigGroup.isUseLinkBasedParkPressure()){

                if (hamburgExperimentalConfigGroup.getParkPressureLinkAttributeFile() != null) {
                    log.info("Adding missing park pressure link attributes based on provided files...");
                    NetworkParkPressureReader networkParkPressureReader = new NetworkParkPressureReader(scenario.getNetwork(),hamburgExperimentalConfigGroup.getParkPressureLinkAttributeFile());
                    Double[] parkTime = Arrays.stream(hamburgExperimentalConfigGroup.getParkPressureBasedParkTime().split(","))
                            .map(Double::parseDouble)
                            .toArray(Double[]::new);
                    networkParkPressureReader.addLinkParkTimeAsAttribute(parkTime);
                    log.info("Adding missing park pressure link attributes based on provided files... Done.");
                }

            }


            // add parkPressureAttribute
            if(hamburgExperimentalConfigGroup.isUseLinkBasedParkPressure()){
                if (hamburgExperimentalConfigGroup.getParkPressureLinkAttributeFile() != null) {
                    log.info("Adding missing park pressure link attributes based on provided files...");
                    NetworkParkPressureReader networkParkPressureReader = new NetworkParkPressureReader(scenario.getNetwork(),hamburgExperimentalConfigGroup.getParkPressureLinkAttributeFile());
                    Double[] parkTime = Arrays.stream(hamburgExperimentalConfigGroup.getParkPressureBasedParkTime().split(","))
                            .map(Double::parseDouble)
                            .toArray(Double[]::new);
                    networkParkPressureReader.addLinkParkTimeAsAttribute(parkTime);
                    log.info("Adding missing park pressure link attributes based on provided files... Done.");
                }
            }


            // add Freight if no filter
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
            } else
                AdjustScenarioForFreight.adjustScenarioForFreight(scenario, AdjustScenarioForFreight.getFreightModes());

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
            ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);
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
            return config;
        }
    }




*/
