package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.sharingFare.SharingFareHandler;
import org.matsim.sharingFare.SharingFaresConfigGroup;
import org.matsim.sharingFare.SharingServiceFaresConfigGroup;
import org.matsim.sharingFare.TeleportedSharingFareHandler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunSharingScenario.*;

/**
 * @author zmeng
 */
public class RunSharingScenarioTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
                "scenarios/input/sharing/hamburg-v2.0-10pct-sharing.config.xml" ,
                "--config:controler.lastIteration" , "0",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:network.inputNetworkFile" , "../../../test/input/test-hamburg-with-pt-network.xml.gz",
                "--config:plans.inputPlansFile" , "test/input/plans/test-scar-sharing-user.plans.xml",
        };

        Config config = prepareConfig(args);


        config.transit().setUseTransit(false);

        config.controler().setRunId("runTest");
        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setOutputDirectory(utils.getOutputDirectory());


        SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config,SharingConfigGroup.class);
        for (SharingServiceConfigGroup service : sharingConfigGroup.getServices()) {
            switch (service.getMode()){
                case "sbike":
                    service.setServiceInputFile("../../../test/input/shared_bike_vehicles_stations.xml");
                    break;
                case "scar":
                    service.setServiceInputFile("../../../test/input/shared_car_vehicles_stations.xml");
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }


        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);


        controler.run();


    }
}
