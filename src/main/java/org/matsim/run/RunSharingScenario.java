package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.sharing.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.sharing.io.ImmutableSharingStationSpecification;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.escooter.EScooterCharger;
import org.matsim.escooter.EScooterConfigGroup;
import org.matsim.escooter.EScooterTeleportationRoutingModule;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zmeng
 */
public class RunSharingScenario {

    private static final Logger log = Logger.getLogger(RunRealLabHH2030Scenario.class);

    private static final String SHARING_CAR = "car";
    private static final String SHARING_BIKE = "bike";

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

        controler.addOverridingModule(new SharingModule());
        controler.configureQSimComponents(SharingUtils.configureQSim(ConfigUtils.addOrGetModule(scenario.getConfig(),SharingConfigGroup.class)));

        return controler;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {

        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);

        ConfigUtils.addOrGetModule(config, SharingConfigGroup.class);
        //add sharing config group
        SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config,SharingConfigGroup.class);

        // define a car sharing service
        SharingServiceConfigGroup carSharingConfig = new SharingServiceConfigGroup();
        sharingConfigGroup.addService(carSharingConfig);
        carSharingConfig.setId(SHARING_CAR);
        carSharingConfig.setMaximumAccessEgressDistance(1000);
        carSharingConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        carSharingConfig.setServiceAreaShapeFile(null);
        carSharingConfig.setServiceInputFile("/Users/meng/IdeaProjects/matsim-hamburg/test/input/shared_car_vehicles_stations.xml");
        carSharingConfig.setMode(TransportMode.car);


        // define a bike sharing service
        SharingServiceConfigGroup bikeSharingConfig = new SharingServiceConfigGroup();
        sharingConfigGroup.addService(bikeSharingConfig);
        bikeSharingConfig.setId(SHARING_BIKE);
        bikeSharingConfig.setMaximumAccessEgressDistance(1000);
        bikeSharingConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        bikeSharingConfig.setServiceAreaShapeFile(null);
        bikeSharingConfig.setServiceInputFile("/Users/meng/IdeaProjects/matsim-hamburg/test/input/shared_bike_vehicles_stations.xml");
        bikeSharingConfig.setMode(TransportMode.bike);



        // add sharing modes to mode choice
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(carSharingConfig));
 //       modes.add(SharingUtils.getServiceMode(bikeSharingConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

        // We need to add interaction activity types to scoring
        PlanCalcScoreConfigGroup.ActivityParams pickupParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.PICKUP_ACTIVITY);
        pickupParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(pickupParams);

        PlanCalcScoreConfigGroup.ActivityParams dropoffParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
        dropoffParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(dropoffParams);

        PlanCalcScoreConfigGroup.ActivityParams bookingParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.BOOKING_ACTIVITY);
        bookingParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(bookingParams);

        return config;
    }

    public static Scenario prepareScenario(Config config) throws IOException {
        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        return scenario;
    }

}
