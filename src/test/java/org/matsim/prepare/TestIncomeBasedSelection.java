/*
package org.matsim.prepare;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.reallabHHPolicyScenarios.RunBaseCaseWithMobilityBudget.*;

public class TestIncomeBasedSelection {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void runTest() throws IOException {

        String[] args = new String[]{
                "test/input//test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "4",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "plans/test-hamburg.plans.xml",
        };


        Config config = prepareConfig(args);
        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        Scenario scenario = prepareScenario(config);

        for (Person p: scenario.getPopulation().getPersons().values()) {
            if (!p.getId().toString().contains("commercial")) {
                personsEligibleForMobilityBudget.put(p.getId(), (double) p.getAttributes().getAttribute("income"));
            }
        }

        int originalSize = personsEligibleForMobilityBudget.size();

        SelectionMobilityBudget.incomeBasedSelection(scenario.getPopulation(), 0.25, personsEligibleForMobilityBudget);

        Assert.assertEquals(originalSize, personsEligibleForMobilityBudget.size()+3);
    }
}
*/
