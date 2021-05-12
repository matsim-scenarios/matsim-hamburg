package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

/**
 * @author zmeng
 */
public class RunHamburgScenarioTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
          "test/input/test-hamburg.config-tem.xml" ,
                "--config:controler.lastIteration" , "1",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.date","2019-06-13",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey","",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:HereAPITravelTimeValidation.numOfTrips","5",
                "--config:HereAPITravelTimeValidation.timeBin","3600",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "test-hamburg-freight.plans-tem.xml",

        };

        Config config = prepareConfig(args);

        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);

        controler.run();


    }

}