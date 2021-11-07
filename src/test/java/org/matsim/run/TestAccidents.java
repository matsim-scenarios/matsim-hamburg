/*
package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.accidents.RunAccidentsHamburg;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

public class TestAccidents {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void RunAccidentsTest() throws IOException {
        String[] args = new String[]{
                "test/input//test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "0",
                "--config:controler.runId" , "AccidentTest",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "plans/test-hamburg.plans.xml",

        };

        RunAccidentsHamburg.main(args);

    }
}
*/
