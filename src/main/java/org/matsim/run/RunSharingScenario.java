package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.sharingFare.SharingFaresConfigGroup;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author zmeng, tschlenther
 */
public class RunSharingScenario {

    private static final Logger log = Logger.getLogger(RunSharingScenario.class);

    public static final String VERSION = "v2.0";

    private static final String SHARING_SERVICE_ID_CAR = "car";
    private static final String SHARING_SERVICE_ID_BIKE = "bike";

    private static final String SHARING_CAR_MODE = "scar";
    private static final String SHARING_BIKE_MODE = "sbike";

    private static final String SERVICE_AREA = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg_city/hamburg_stadtteil.shp";

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"scenarios/input/hamburg-v1.1-10pct.config.xml"};
        }

        RunSharingScenario sharingScenario = new RunSharingScenario();
        sharingScenario.run(args);
    }

    private void run(String[] args) throws IOException {

        Config config = prepareConfig(args);
        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();
        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        Controler controler =  RunBaseCaseHamburgScenario.prepareControler(scenario);

        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
//                addTravelTimeBinding("scar").to(networkTravelTime());
//                addTravelDisutilityFactoryBinding("scar").to(carTravelDisutilityFactoryKey());

                //we need to bind a routing module for the main stage of our newly introduced sharing mode. and it is PARTICULARLY IMPORTANT to bind it, before we bind the corresponding sharing module
                //bc otherwise, the sharing module routing will be overwritten
                addRoutingModuleBinding(SHARING_CAR_MODE).toProvider(new NetworkRoutingProvider(SHARING_CAR_MODE));

                PlansCalcRouteConfigGroup.ModeRoutingParams sbike = controler.getConfig().plansCalcRoute().getModeRoutingParams().get("sbike");
                addRoutingModuleBinding(SHARING_BIKE_MODE).toInstance(new TeleportationRoutingModule(SHARING_BIKE_MODE,scenario,sbike.getTeleportedModeSpeed(),sbike.getBeelineDistanceFactor()));
            }
        });

        controler.addOverridingModule(new SharingModule());
        controler.configureQSimComponents(SharingUtils.configureQSim(ConfigUtils.addOrGetModule(scenario.getConfig(),SharingConfigGroup.class)));

//        controler.addOverridingModule(new AbstractModule() {
//            @Override
//            public void install() {
//                this.addEventHandlerBinding().toInstance(new SharingFareHandler(SHARING_SERVICE_ID_CAR));
//                this.addEventHandlerBinding().toInstance(new TeleportedSharingFareHandler(SHARING_SERVICE_ID_BIKE));
//            }
//        });


        return controler;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {

        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);

        ConfigUtils.addOrGetModule(config, SharingConfigGroup.class);
        ConfigUtils.addOrGetModule(config, SharingFaresConfigGroup.class);
        //add sharing config group
        SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config,SharingConfigGroup.class);

        // define a car sharing service
        SharingServiceConfigGroup carSharingConfig = new SharingServiceConfigGroup();
        sharingConfigGroup.addService(carSharingConfig);
        carSharingConfig.setId(SHARING_SERVICE_ID_CAR);
        carSharingConfig.setMaximumAccessEgressDistance(2000);
        carSharingConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        carSharingConfig.setServiceAreaShapeFile(SERVICE_AREA);
        carSharingConfig.setServiceInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/input/sharing/sharingStationsAndSharingVehicles_scar.xml");
        carSharingConfig.setMode(SHARING_CAR_MODE);


        // define a bike sharing service
        SharingServiceConfigGroup bikeSharingConfig = new SharingServiceConfigGroup();
        sharingConfigGroup.addService(bikeSharingConfig);
        bikeSharingConfig.setId(SHARING_SERVICE_ID_BIKE);
        bikeSharingConfig.setMaximumAccessEgressDistance(1000);
        bikeSharingConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        bikeSharingConfig.setServiceAreaShapeFile(SERVICE_AREA);
        bikeSharingConfig.setServiceInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/input/sharing/sharingStationsAndSharingVehicles_sbike.xml");
        bikeSharingConfig.setMode(SHARING_BIKE_MODE);

        // add sharing modes to mode choice
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(carSharingConfig));
        modes.add(SharingUtils.getServiceMode(bikeSharingConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

        PlanCalcScoreConfigGroup.ActivityParams pickupParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.PICKUP_ACTIVITY);
        pickupParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(pickupParams);

        PlanCalcScoreConfigGroup.ActivityParams dropoffParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
        dropoffParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(dropoffParams);

        PlanCalcScoreConfigGroup.ActivityParams bookingParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.BOOKING_ACTIVITY);
        bookingParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(bookingParams);

        // add modeParams for scar and sbike
        {
            PlanCalcScoreConfigGroup.ModeParams scarParams = new PlanCalcScoreConfigGroup.ModeParams(SHARING_CAR_MODE);
            //copy relevant parameters from car. monetary components will be handled via the fares though
            PlanCalcScoreConfigGroup.ModeParams carParams = config.planCalcScore().getModes().get(TransportMode.car);
            scarParams.setConstant(carParams.getConstant());
            scarParams.setMarginalUtilityOfTraveling(carParams.getMarginalUtilityOfTraveling());
            scarParams.setDailyUtilityConstant(carParams.getDailyUtilityConstant());
            scarParams.setMarginalUtilityOfDistance(carParams.getMarginalUtilityOfDistance());

            PlanCalcScoreConfigGroup.ModeParams sbikeParams = new PlanCalcScoreConfigGroup.ModeParams(SHARING_BIKE_MODE);
            //copy relevant parameters from bike. monetary components will be handled via the fares though
            PlanCalcScoreConfigGroup.ModeParams bikeParams = config.planCalcScore().getModes().get(TransportMode.bike);
            sbikeParams.setConstant(bikeParams.getConstant());
            sbikeParams.setMarginalUtilityOfTraveling(bikeParams.getMarginalUtilityOfTraveling());
            sbikeParams.setDailyUtilityConstant(bikeParams.getDailyUtilityConstant());
            sbikeParams.setMarginalUtilityOfDistance(bikeParams.getMarginalUtilityOfDistance());
            //TODO this comes from the stadtrad fares. the service costs 5 euros per year
            sbikeParams.setDailyMonetaryConstant( (5. / 365.) );

            config.planCalcScore().addModeParams(sbikeParams);
            config.planCalcScore().addModeParams(scarParams);
        }

        //add scar as network modes
        Set<String> networkModes = new HashSet<>();
        networkModes.addAll(config.plansCalcRoute().getNetworkModes());
        networkModes.add(SHARING_CAR_MODE);
        config.plansCalcRoute().setNetworkModes(networkModes);

        //add sbike as a teleportation mode
        PlansCalcRouteConfigGroup.TeleportedModeParams sbikeTeleParams = new PlansCalcRouteConfigGroup.TeleportedModeParams(SHARING_BIKE_MODE);
        sbikeTeleParams.setBeelineDistanceFactor(1.45);
        sbikeTeleParams.setTeleportedModeSpeed(3.138889);
        config.plansCalcRoute().addTeleportedModeParams(sbikeTeleParams);

        //add scar to mainMode in qsim
        Set<String> mainMode = new HashSet<>();
        mainMode.addAll(config.qsim().getMainModes());
        mainMode.add(SHARING_CAR_MODE);
        config.qsim().setMainModes(mainMode);

        //configure sharing fares
        {
            //this has not worked and the sharing contribution provides now it's own fare implementation
//        SharingFaresConfigGroup sharingFaresConfigGroup = ConfigUtils.addOrGetModule(config, SharingFaresConfigGroup.class);
//
//        SharingServiceFaresConfigGroup scarFares = new SharingServiceFaresConfigGroup();
//        scarFares.setId(SHARING_SERVICE_ID_CAR);
//        scarFares.setBasefare(1);
//        scarFares.setTimeFare_m(0.5);
//        scarFares.setDistanceFare_m(0.);
//        sharingFaresConfigGroup.addServiceFaresParams(scarFares);
//
//        SharingServiceFaresConfigGroup sbikeFares = new SharingServiceFaresConfigGroup();
//        scarFares.setId(SHARING_SERVICE_ID_BIKE);
//        scarFares.setBasefare(0.79);
//        scarFares.setTimeFare_m(0.19);
//        scarFares.setDistanceFare_m(0.);
//        sharingFaresConfigGroup.addServiceFaresParams(sbikeFares);

            //these values are set according to shareNow fares ass of July 2021
            carSharingConfig.setBaseFare(0.);
            carSharingConfig.setMinimumFare(0.);
            carSharingConfig.setTimeFare( (0.29 / 60.) ); //per second. maybe is some kind of special tariff and we should rather use 0.28€/min
            carSharingConfig.setDistanceFare(0.);

            //these values are set according to stadtrad fares ass of July 2021
            //actually, the service is 30 mins for free and then costs 0.10 €/min. We can not really model this right now...
            //and we model the yearly subscription fee in the mode params, see above.
            bikeSharingConfig.setBaseFare(0.);
            bikeSharingConfig.setMinimumFare(0.);
            bikeSharingConfig.setTimeFare(0.); //per second. costs actually 0.10/min after the 30th minute...
            bikeSharingConfig.setDistanceFare(0.); //per meter
        }

        //add share modes to subtourModeChoice
        List<String> subtourModes = new ArrayList(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(carSharingConfig));
        modes.add(SharingUtils.getServiceMode(bikeSharingConfig));

        //set new vehicles file
        config.vehicles().setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/input/hamburg-v2-vehicle-types.xml");

        return config;
    }

    public static Scenario prepareScenario(Config config) throws IOException {
        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        Network network = scenario.getNetwork();
        for (Link link : network.getLinks().values()) {
            if(link.getAllowedModes().contains("car") || link.getAllowedModes().contains("bike")){
                var allowedModes = link.getAllowedModes();
                HashSet<String> newAllowedModes = new HashSet<>();
                newAllowedModes.addAll(allowedModes);
                if(link.getAllowedModes().contains("car"))
                    newAllowedModes.add(SHARING_CAR_MODE);
                if(link.getAllowedModes().contains("bike"))
                    newAllowedModes.add(SHARING_BIKE_MODE);

                link.setAllowedModes(newAllowedModes);
            }
        }

        return scenario;
    }

}
