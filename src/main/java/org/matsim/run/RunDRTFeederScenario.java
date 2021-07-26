package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.prepare.ClassifyStationType;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tschlenther
 */
public class RunDRTFeederScenario {

    private static final Logger log = Logger.getLogger(RunDRTFeederScenario.class);

    public static final String DRT_FEEDER_MODE = "drt_feeder";
    private static final String DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE = "drtStopFilter";
    private static final String DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE = "HVV_switch_drtServiceArea";

    //TODO: could in fact take the same shape as the rebalancing zones shape file since the latter covers in fact (currently) the same are but is just split into more polygons...
    public static final String DRT_FEEDER_SERVICE_AREA = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/input/drtFeeder/serviceArea/hamburg-v2.0-drt-feeder-service-areas.shp";
    private static final String DRT_FEEDER_REBALANCING_ZONES = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/input/drtFeeder/rebalancing/service-area-divided-1000m.shp";
    private static String DRT_FEEDER_VEHICLES = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/input/drtFeeder/vehicles/hamburg-v2.0-drt-feeder-by-rndLocations-1000vehicles-8seats.xml.gz";
    private static final String ALL_DRT_OPERATION_AREA = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg_city/hamburg_stadtteil.shp";

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"scenarios/input/hamburg-v1.1-1pct.config.xml"};
        } else {
            DRT_FEEDER_VEHICLES = args[0];
            String[] tmp = new String[args.length - 1];
            for (int i = 1; i < args.length; i++){
                tmp[i-1] = args[i];
            }
            args = tmp;
        }


        RunDRTFeederScenario realLabHH2030 = new RunDRTFeederScenario();
        realLabHH2030.run(args);
    }

    private void run(String[] args) throws IOException {

        Config config = prepareConfig(args);

//        //set real (1pct) input plans
//        config.plans().setInputFile("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v1/hamburg-v1.1/input/hamburg-v1.1-1pct.plans.xml.gz");

        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();
        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        Controler controler =  RunBaseCaseHamburgScenario.prepareControler(scenario);

//        // drt + dvrp module
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                // use a main mode identifier which knows how to handle intermodal trips generated by the used sbb pt raptor router
                // the SwissRailRaptor already binds its IntermodalAwareRouterModeIdentifier, however drt obviously replaces it
                // with its own implementation
                // So we need our own main mode indentifier which replaces both :-(
                //TODO: write our hamburg mainModeIdentifier,which can deal with all the pt+x(s)

                //HamburgAnalysisMainModeIdentifier was already bound
//                bind(MainModeIdentifier.class).toInstance(new TransportPlanningMainModeIdentifier());
//                bind(AnalysisMainModeIdentifier.class).to(HamburgAnalysisMainModeIdentifier.class);

                //need to bind this in another overriding module than in the module where we install the SwissRailRaptorModule
                bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
            }
        });

        controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        controler.addOverridingModule(new PtIntermodalRoutingModesModule());

        return controler;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        ConfigGroup[] customModulesToAdd = new ConfigGroup[] { new DvrpConfigGroup(), new MultiModeDrtConfigGroup(),
                new SwissRailRaptorConfigGroup(), new IntermodalTripFareCompensatorsConfigGroup(),
                new PtIntermodalRoutingModesConfigGroup()};
        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModulesAll);

        configureDRTFeeder(config);

        return config;
    }

    //TODO: make this all configurable via xml and move it out of the code
    public static Config configureDRTFeeder(Config config){
        //when simulating dvrp, we need/should simulate from the start to the end
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

        DvrpConfigGroup dvrpCfg = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
        dvrpCfg.setNetworkModes(ImmutableSet.<String>builder()
                .add(DRT_FEEDER_MODE)
                .build());
        //TODO potentially further configure dvrp. For example, travelTimeMatrix cell size etc.

        MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

        DrtConfigGroup drtFeederCfg = new DrtConfigGroup();
        drtFeederCfg.setMode(DRT_FEEDER_MODE);
        drtFeederCfg.setOperationalScheme(DrtConfigGroup.OperationalScheme.serviceAreaBased);
        drtFeederCfg.setUseModeFilteredSubnetwork(true); //vehicles should be able to drive from one area to the other
        drtFeederCfg.setMaxWaitTime(300); //5 minutes as in the ReallabHH goals
        drtFeederCfg.setRejectRequestIfMaxWaitOrTravelTimeViolated(false); //no rejections please

        //service area
        drtFeederCfg.setDrtServiceAreaShapeFile(DRT_FEEDER_SERVICE_AREA);

        //operation area (area where all drt modes are allowed on the network
        HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);
        hamburgExperimentalConfigGroup.setDrtOperationArea(ALL_DRT_OPERATION_AREA);

        //fleet
        drtFeederCfg.setVehiclesFile(DRT_FEEDER_VEHICLES);

        {//fare
            DrtFareParams drtFeederFareParams = new DrtFareParams();
            drtFeederFareParams.setBasefare(1.);
            drtFeederFareParams.setDistanceFare_m(0.);
            drtFeederFareParams.setTimeFare_h(0.);
            drtFeederFareParams.setMinFarePerTrip(1.);
            drtFeederFareParams.setDailySubscriptionFee(0.);
            drtFeederCfg.addParameterSet(drtFeederFareParams);
        }


        {//rebalancing
            //general
            RebalancingParams rebalancingParams = new RebalancingParams();
            rebalancingParams.setInterval(600); //1800 is default. should prbly be the same as demand estimation period

            {//algorithm
                MinCostFlowRebalancingStrategyParams mincostFlowParams = new MinCostFlowRebalancingStrategyParams();
                mincostFlowParams.setRebalancingTargetCalculatorType(MinCostFlowRebalancingStrategyParams.RebalancingTargetCalculatorType.EstimatedDemand); //Estimated Demand is default
                mincostFlowParams.setZonalDemandEstimatorType(MinCostFlowRebalancingStrategyParams.ZonalDemandEstimatorType.PreviousIterationDemand); //previousIterationDemand is default
                mincostFlowParams.setDemandEstimationPeriod(600); //1800 is default. should prbly be the same as interval
                mincostFlowParams.setTargetAlpha(0.5); //0.5 is default
                mincostFlowParams.setTargetBeta(0.5); //0.5 is default
                rebalancingParams.addParameterSet(mincostFlowParams);

//            PlusOneRebalancingStrategyParams plusOneRebalancingStrategyParams = new PlusOneRebalancingStrategyParams();
//            rebalancingParams.addParameterSet(plusOneRebalancingStrategyParams);
            }

            {//zones
                DrtZonalSystemParams rebalancingZones = new DrtZonalSystemParams();
                rebalancingZones.setZonesGeneration(DrtZonalSystemParams.ZoneGeneration.ShapeFile);
                rebalancingZones.setTargetLinkSelection(DrtZonalSystemParams.TargetLinkSelection.random);
                rebalancingZones.setZonesShapeFile(DRT_FEEDER_REBALANCING_ZONES);
                drtFeederCfg.addParameterSet(rebalancingZones);
            }

            //add rebalancing params
            drtFeederCfg.addParameterSet(rebalancingParams);
        }

        //set some standard values
        drtFeederCfg.setMaxTravelTimeAlpha(1.7);
        drtFeederCfg.setMaxTravelTimeBeta(120);
        drtFeederCfg.setStopDuration(60);
        drtFeederCfg.addDrtInsertionSearchParams(new ExtensiveInsertionSearchParams());

        multiModeDrtCfg.addDrtConfig(drtFeederCfg);

        //configure drt speed-up params
        for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
            if (drtCfg.getDrtSpeedUpParams().isEmpty()) {
                drtCfg.addParameterSet(new DrtSpeedUpParams());
            }
        }

        //add drt stage activities
        DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtCfg, config.planCalcScore(), config.plansCalcRoute());

        //add mode params
        PlanCalcScoreConfigGroup.ModeParams carParams = config.planCalcScore().getModes().get(TransportMode.car);

        PlanCalcScoreConfigGroup.ModeParams drtFeederModeParams = new PlanCalcScoreConfigGroup.ModeParams(DRT_FEEDER_MODE);
        //these values come from project partners. TODO: check if correct
        drtFeederModeParams.setConstant(carParams.getConstant() - 0.827); //car should be 0
//        drtFeederModeParams.setMarginalUtilityOfTraveling(-3.396); //TODO talk to KN
        //copy from car
        drtFeederModeParams.setMarginalUtilityOfDistance(carParams.getMarginalUtilityOfDistance()); //should be 0
        config.planCalcScore().addModeParams(drtFeederModeParams);

        //configure intermodal pt
        SwissRailRaptorConfigGroup swissRailRaptorConfigGroup = ConfigUtils.addOrGetModule(config,SwissRailRaptorConfigGroup.class);
        swissRailRaptorConfigGroup.setUseIntermodalAccessEgress(true);

        //add drtFeeder
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet drtFeederAccessEgressParameterSet = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        drtFeederAccessEgressParameterSet.setMode(DRT_FEEDER_MODE);
        drtFeederAccessEgressParameterSet.setStopFilterAttribute(DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE);
        drtFeederAccessEgressParameterSet.setStopFilterValue(DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE);
        //TODO these values were recommended by GL based on his experiences for Berlin
        drtFeederAccessEgressParameterSet.setInitialSearchRadius(3_000);
        drtFeederAccessEgressParameterSet.setSearchExtensionRadius(1_000);
        drtFeederAccessEgressParameterSet.setMaxRadius(20_000);
        swissRailRaptorConfigGroup.addIntermodalAccessEgress(drtFeederAccessEgressParameterSet);

        //add walk
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet walkAccessEgressParameterSet = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        walkAccessEgressParameterSet.setMode(TransportMode.walk);
        //TODO these values were recommended copied from GL
        walkAccessEgressParameterSet.setInitialSearchRadius(1_500);
        walkAccessEgressParameterSet.setSearchExtensionRadius(1_000);
        walkAccessEgressParameterSet.setMaxRadius(100_000);
        swissRailRaptorConfigGroup.addIntermodalAccessEgress(walkAccessEgressParameterSet);

        return config;
    }

    public static Scenario prepareScenario(Config config) throws IOException {
        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);
        for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
            //TODO: this is not the best solution

            // Michal says restricting drt to a drt network roughly the size of the service area helps to speed up.
            // This is even more true since drt started to route on a freespeed TT matrix (Nov '20).
            // A buffer of 10km to the service area Berlin includes the A10 on some useful stretches outside Berlin.
            if(hamburgExperimentalConfigGroup.getDrtOperationArea() != null){
                addDRTmode(scenario, drtCfg.getMode(), hamburgExperimentalConfigGroup.getDrtOperationArea(), 0.);
            }

            if(drtCfg.getMode().equals(DRT_FEEDER_MODE)){
                //tag pt stops that are to be used for intermodal access and egress
                //TODO restrict to the actual stations that we want to use and do not use generic solution here.... (restrict to rail stations)
                tagTransitStopsInServiceArea(scenario.getTransitSchedule(),
                        DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE, DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE,
                        drtCfg.getDrtServiceAreaShapeFile(),
                        /* "stopFilter", "station_S/U/RE/RB",*/
                        0.); //
            }
        }
        return scenario;
    }

    public static void addDRTmode(Scenario scenario, String drtNetworkMode, String drtServiceAreaShapeFile, double buffer) {

        log.info("Adjusting network...");

        HamburgShpUtils shpUtils = new HamburgShpUtils( drtServiceAreaShapeFile );

        int counter = 0;
        int counterInside = 0;
        int counterOutside = 0;
        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (counter % 10000 == 0)
                log.info("link #" + counter);
            counter++;
            if (link.getAllowedModes().contains(TransportMode.car)) {
                if (shpUtils.isCoordInDrtServiceAreaWithBuffer(link.getFromNode().getCoord(), buffer)
                        || shpUtils.isCoordInDrtServiceAreaWithBuffer(link.getToNode().getCoord(), buffer)) {
                    Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

                    allowedModes.add(drtNetworkMode);

                    link.setAllowedModes(allowedModes);
                    counterInside++;
                } else {
                    counterOutside++;
                }

            } else if (link.getAllowedModes().contains(TransportMode.pt)) {
                // skip pt links
            } else {
                throw new RuntimeException("Aborting...");
            }
        }

        log.info("Total links: " + counter);
        log.info("Total links inside service area: " + counterInside);
        log.info("Total links outside service area: " + counterOutside);

        Set<String> modes = new HashSet<>();
        modes.add(drtNetworkMode);
        new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
    }

    private static void tagTransitStopsInServiceArea(TransitSchedule transitSchedule,
                                                     String newAttributeName, String newAttributeValue,
                                                     String drtServiceAreaShapeFile,
                                                     /*String oldFilterAttribute, String oldFilterValue,*/
                                                     double bufferAroundServiceArea) {
        log.info("Tagging pt stops marked for intermodal access/egress in the service area.");
        HamburgShpUtils shpUtils = new HamburgShpUtils( drtServiceAreaShapeFile );

        //TODO this rather ugly classification implementation should be replaced by GL's version which directly puts attributes into the transitSchedule
        ClassifyStationType.getStop2TypesList(transitSchedule).stream()
                .filter(stop2type -> stop2type.getType() != null && (stop2type.getType().contains("s") || stop2type.getType().contains("r")) ) //filter rail and subway stations
                .filter(stop2Type -> shpUtils.isCoordInDrtServiceAreaWithBuffer(stop2Type.getStop().getCoord(), bufferAroundServiceArea)) //filter spatially
                .forEach(stop2type -> stop2type.getStop().getAttributes().putAttribute(newAttributeName, newAttributeValue));


//        for (TransitStopFacility stop: transitSchedule.getFacilities().values()) {
//
//
//            /*if (stop.getAttributes().getAttribute(oldFilterAttribute) != null) {
//                if (stop.getAttributes().getAttribute(oldFilterAttribute).equals(oldFilterValue)) {*/
//                    if (shpUtils.isCoordInDrtServiceAreaWithBuffer(stop.getCoord(), bufferAroundServiceArea)) {
//                        stop.getAttributes().putAttribute(newAttributeName, newAttributeValue);
//                    }
//                }
//            }
//        }
    }
}
