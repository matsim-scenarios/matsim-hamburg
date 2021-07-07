package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.run.RunBaseCaseWithMobilityBudgetV2.*;

/**
 * @author Gryb
 */
public class RunHamburgScenarioMobilityBudgetTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "1",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.date","2019-06-13",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey","",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:HereAPITravelTimeValidation.numOfTrips","5",
                "--config:HereAPITravelTimeValidation.timeBin","3600",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:plans.inputPlansFile" , "test-hamburg-freight.plans.xml",

        };

        Config config = prepareConfig(args);
        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);
        controler.run();

        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
        HashMap<Id<Person>, Double> scoreStatsFromBaseCase = new HashMap<>();
        scoreStatsFromBaseCase.put(Id.createPersonId("113ecc"),115.34333505696776);
        //Agent stays at home the whole day so doesn´t use his car still gets the MobBud
        scoreStatsFromBaseCase.put(Id.createPersonId("113efb"),10.0);
        scoreStatsFromBaseCase.put(Id.createPersonId("113f00"),47.10954448365045);
        scoreStatsFromBaseCase.put(Id.createPersonId("113f02"),127.86871825413606);
        scoreStatsFromBaseCase.put(Id.createPersonId("commercial_1000074"),121.90659700031605);

        for (Person p: persons.values()) {
            Assert.assertEquals(scoreStatsFromBaseCase.get(p.getId()), p.getSelectedPlan().getScore(), 0);
        }

    }

}