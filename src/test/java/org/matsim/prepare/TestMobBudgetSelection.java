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

public class TestMobBudgetSelection {

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
                "--config:plans.inputPlansFile", "plans/test-hamburg-freight.plans.xml",
        };


        Config config = prepareConfig(args);
        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        String shapeFile = "C:\\Users\\Gregor\\Documents\\shared-svn\\projects\\realLabHH\\data\\hamburg_shapeFile\\hamburg_metropo\\hamburg_metropo.shp";
        Scenario scenario = prepareScenario(config);
        int originalSize = personsEligibleForMobilityBudget.size();
        for (Person p: scenario.getPopulation().getPersons().values()) {
            Plan plan = p.getSelectedPlan();
            List <Activity>  activitiesList = TripStructureUtils.getActivities(plan.getPlanElements(),TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
            for (Activity activity: activitiesList) {
                if (activity.getType().contains("home")) {
                    Coord coordOfHomeActivity = new Coord(565277.394206, 5933737,159015);
                    activity.setCoord(coordOfHomeActivity);
                    // Agent used to get the mobilityBudget but not now
                    if (p.getId().toString().contains("113ecc")) {
                        Coord coordOfHomeActivityOutOfShapeFile = new Coord(6065277.394206, 60933737,159015);
                        activity.setCoord(coordOfHomeActivityOutOfShapeFile);
                    }
                }
            }
        }
        SelectionMobilityBudget.filterForRegion(scenario.getPopulation(), shapeFile,personsEligibleForMobilityBudget);
        Assert.assertEquals(originalSize, personsEligibleForMobilityBudget.size()+1);
    }
}
