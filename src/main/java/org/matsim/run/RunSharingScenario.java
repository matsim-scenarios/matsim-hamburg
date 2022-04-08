package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.sharing.SharingIdleVehiclesXYWriter;
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

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author zmeng, tschlenther
 *
 * uses infrastructure from the sharing contrib and incorporates a bike sharing and a car sharing service that operate in freeflow mode.
 * user costs are based on real data from 2021.
 * scoring mode parameters are copied from bike and car.
 */
public class RunSharingScenario {

    private static final Logger log = Logger.getLogger(RunSharingScenario.class);

    public static final String SHARING_SERVICE_ID_CAR = "car";
    public static final String SHARING_SERVICE_ID_BIKE = "bike";

    public static final String SHARING_CAR_MODE = "scar";
    public static final String SHARING_BIKE_MODE = "sbike";

    private static final String SERVICE_AREA = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg_city/hamburg_stadtteil.shp";

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-25pct.config.baseCase.xml"};
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
        return addModulesAndConfigureQSim(scenario, controler);
    }

    static Controler addModulesAndConfigureQSim(Scenario scenario, Controler controler) {
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
//                addTravelTimeBinding("scar").to(networkTravelTime());
//                addTravelDisutilityFactoryBinding("scar").to(carTravelDisutilityFactoryKey());

                //we need to bind a routing module for the main stage of our newly introduced sharing mode. and it is PARTICULARLY IMPORTANT to bind it, before we bind the corresponding sharing module
                //bc otherwise, the sharing module routing will be overwritten
                addRoutingModuleBinding(SHARING_CAR_MODE).toProvider(new NetworkRoutingProvider(SHARING_CAR_MODE));

                PlansCalcRouteConfigGroup.ModeRoutingParams sbike = controler.getConfig().plansCalcRoute().getModeRoutingParams().get(SHARING_BIKE_MODE);
                addRoutingModuleBinding(SHARING_BIKE_MODE).toInstance(new TeleportationRoutingModule(SHARING_BIKE_MODE, scenario, sbike.getTeleportedModeSpeed(),sbike.getBeelineDistanceFactor()));

                SharingIdleVehiclesXYWriter idleBikesWriter = new SharingIdleVehiclesXYWriter(SHARING_SERVICE_ID_BIKE, scenario.getNetwork());
                addEventHandlerBinding().toInstance(idleBikesWriter);
                addControlerListenerBinding().toInstance(idleBikesWriter);

                SharingIdleVehiclesXYWriter idleCarsWriter = new SharingIdleVehiclesXYWriter(SHARING_SERVICE_ID_CAR, scenario.getNetwork());
                addEventHandlerBinding().toInstance(idleCarsWriter);
                addControlerListenerBinding().toInstance(idleCarsWriter);
            }
        });

        controler.addOverridingModule(new SharingModule());
        controler.configureQSimComponents(SharingUtils.configureQSim(ConfigUtils.addOrGetModule(controler.getConfig(),SharingConfigGroup.class)));

        return controler;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);
        return configureBikeAndCarSharingServices(config);
    }

    public static Config configureBikeAndCarSharingServices(Config config) {
        //the SharingServiceConfigGroups (and SharingConfigGroup) can not be read from xml yet!
        //This is why we set the input files from our experimental config groups. I know, it is ugly.... tschlenther sep '21.

        //add sharing config group
        SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config,SharingConfigGroup.class);
        HamburgExperimentalConfigGroup hamburgCfg = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);

        // define a car sharing service
        SharingServiceConfigGroup carSharingConfig = new SharingServiceConfigGroup();
        sharingConfigGroup.addService(carSharingConfig);
        carSharingConfig.setId(SHARING_SERVICE_ID_CAR);
        carSharingConfig.setMaximumAccessEgressDistance(10_000); //high acceptance for lower probability to stuck
        carSharingConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        carSharingConfig.setServiceAreaShapeFile(SERVICE_AREA);
//        carSharingConfig.setServiceInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030/input/sharing/sharingStationsAndSharingVehicles_scar.xml");
        carSharingConfig.setServiceInputFile(hamburgCfg.getCarSharingServiceInputFile()); //see comment above
        carSharingConfig.setMode(SHARING_CAR_MODE);

        // define a bike sharing service
        SharingServiceConfigGroup bikeSharingConfig = new SharingServiceConfigGroup();
        sharingConfigGroup.addService(bikeSharingConfig);
        bikeSharingConfig.setId(SHARING_SERVICE_ID_BIKE);
        bikeSharingConfig.setMaximumAccessEgressDistance(10_000); //high acceptance for lower probability to stuck
        bikeSharingConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        bikeSharingConfig.setServiceAreaShapeFile(SERVICE_AREA);
//        bikeSharingConfig.setServiceInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030/input/sharing/sharingStationsAndSharingVehicles_sbike.xml");
        bikeSharingConfig.setServiceInputFile(hamburgCfg.getBikeSharingServiceInputFile()); //see comment above
        bikeSharingConfig.setMode(SHARING_BIKE_MODE);

        // add sharing modes to mode choice
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(carSharingConfig));
        modes.add(SharingUtils.getServiceMode(bikeSharingConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

        List<String> changeModes = new ArrayList<>(Arrays.asList(config.changeMode().getModes()));
        changeModes.add(SharingUtils.getServiceMode(carSharingConfig));
        changeModes.add(SharingUtils.getServiceMode(bikeSharingConfig));
        config.changeMode().setModes(changeModes.toArray(new String[modes.size()]));

        //configure activities
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
            //this comes from the stadtrad fares. the service costs 5 euros per year
            sbikeParams.setDailyMonetaryConstant( (5. / 230.) ); // circa 230 working days per year (disregarding saturdays) (which is what we model)

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
            //these values are set according to shareNow fares as of July 2021
            carSharingConfig.setBaseFare(0.);
            carSharingConfig.setMinimumFare(0.);
            carSharingConfig.setTimeFare( (0.29 / 60.) ); //0.29 €/ min => per second
            carSharingConfig.setDistanceFare(0.);

            //these values are set according to stadtrad fares ass of July 2021
            //actually, the service is 30 mins for free and then costs 0.10 €/min. We can not really model this right now...
            //and we model the yearly subscription fee in the mode params, see above.
            bikeSharingConfig.setBaseFare(0.);
            bikeSharingConfig.setMinimumFare(0.);
            bikeSharingConfig.setTimeFare(0.1 / 60.); //per second. costs actually 0.10/min after the 30th minute. as we model large infrastructure changes in the scenario, the intake from the first 30 mins could be taken for financing
            bikeSharingConfig.setDistanceFare(0.); //per meter
        }

        //add share modes to subtourModeChoice
        List<String> subtourModes = new ArrayList(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(carSharingConfig));
        modes.add(SharingUtils.getServiceMode(bikeSharingConfig));

        return config;
    }

    public static Scenario prepareScenario(Config config) throws IOException {
        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        addSharingModesToNetwork(scenario.getNetwork());
        return scenario;
    }

    /**
     * adds the mode {@code SHARING_BIKE_MODE} to links where {@code bike} is allowed
     * and {@code SHARING_CAR_MODE} where {@code car} is allowed. Thus, this method has side effects!
     * @param network
     */
    private static void addSharingModesToNetwork(Network network) {
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
    }

}
