package org.matsim.prepare;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.testcases.MatsimTestUtils;
import java.io.IOException;
import java.util.List;

import static org.matsim.run.RunBaseCaseWithMobilityBudgetV2.*;
import static org.matsim.run.RunBaseCaseWithMobilityBudgetV2.personsEligibleForMobilityBudget;

public class TestIncomeBasedSelection {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void runTest() throws IOException {

        String[] args = new String[]{
                "test/input/test-hamburg.config.xml",
                "--config:controler.lastIteration", "4",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.date", "2019-06-13",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey", "",
                "--config:HereAPITravelTimeValidation.useHereAPI", "false",
                "--config:HereAPITravelTimeValidation.numOfTrips", "5",
                "--config:HereAPITravelTimeValidation.timeBin", "3600",
                "--config:hamburgExperimental.useLinkBasedParkPressure", "true",
                "--config:plans.inputPlansFile" , "plans/test-hamburg-withIncome.plans.xml",
        };


        Config config = prepareConfig(args);
        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        Scenario scenario = prepareScenario(config);


        for (Person p: scenario.getPopulation().getPersons().values()) {
            personsEligibleForMobilityBudget.put(p.getId(), (double) p.getAttributes().getAttribute("income"));
        }

        int originalSize = personsEligibleForMobilityBudget.size();

        SelectionMobilityBudget.incomeBasedSelection(scenario.getPopulation(), 0.25, personsEligibleForMobilityBudget);

        Assert.assertEquals(originalSize, personsEligibleForMobilityBudget.size()+3);
    }
}
