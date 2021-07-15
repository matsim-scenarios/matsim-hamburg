package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.run.RunBaseCaseWithMobilityBudgetV2.*;

public class RunHamburgScenarioMobilityBudgetWithIncomeTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
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
        //adjusting strategy setting of config so agents try out different modes
        for (StrategyConfigGroup.StrategySettings setting:    config.strategy().getStrategySettings()) {
            if (setting.getStrategyName().equals("SubtourModeChoice")) {
                setting.setWeight(1.0);
            }
        }
        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        //forcing run class to useIncomeForMobilityBudget with an shareOfIncome of 0.9
        shareOfIncome = 0.9;
        useIncomeForMobilityBudget = true;
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);
        controler.run();

        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
        HashMap<Id<Person>, Double> scoreStatsFromBaseCase = new HashMap<>();
        //Agents used car in BaseCase now switched and got the MobilityBudget (amount = (4500*0.9)/30=135)
        scoreStatsFromBaseCase.put(Id.createPersonId("113ecc"), 164.086211440912123);
        //Agent stays at home the whole day so doesn´t use his car so does not get the MobilityBudget
        scoreStatsFromBaseCase.put(Id.createPersonId("113efb"), 0.0);
        //Agent used car in BaseCase and is still using it --> no MobilityBudget
        scoreStatsFromBaseCase.put(Id.createPersonId("113f00"), 47.09443917204786);
        //Agent didn´t use car in Base Case
        scoreStatsFromBaseCase.put(Id.createPersonId("113f02"), 117.86871825413606);
        //Agent with commercial activity are excluded from the MobilityBudget
        scoreStatsFromBaseCase.put(Id.createPersonId("commercial_1000074"), 121.90659700031605);

        for (Person p : persons.values()) {
            Assert.assertEquals(scoreStatsFromBaseCase.get(p.getId()), p.getSelectedPlan().getScore(), 0);
        }

    }


}
