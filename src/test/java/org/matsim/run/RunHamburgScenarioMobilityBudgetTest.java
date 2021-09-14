package org.matsim.run;

import org.apache.commons.lang.ArrayUtils;
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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.run.RunBaseCaseWithMobilityBudget.*;

/**
 * @author Gryb
 */
public class RunHamburgScenarioMobilityBudgetTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException, ParseException {

        String[] args = new String[]{
                "test/input//test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "4",
                "--config:controler.runId" , "RunBaseCaseHamburgScenarioIT",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "plans/test-hamburg.plans.xml",
        };

        String[] mobBudgetArgs = new String[]{
                "--dailyMobilityBudget" , "100.0",
                "--useIncomeForMobilityBudget" , "false",
                "--shareOfIncome", "0.0",
                "--useShapeFile", "false",
                "--shapeFile","",
                "--incomeBasedSelection","false",
                "--shareOfAgents","1.0",
        };

        String[] both = (String[]) ArrayUtils.addAll(args, mobBudgetArgs);
        //need to call main method
        main(both);
        Config config = prepareConfig(both);

        //adjusting strategy setting of config so agents try out different modes
        for (StrategyConfigGroup.StrategySettings setting:    config.strategy().getStrategySettings()) {
            if (setting.getStrategyName().equals("SubtourModeChoice")) {
                setting.setWeight(1.0);
            }

        }
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);
        controler.run();

        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
        HashMap<Id<Person>, Double> scoreStatsFromBaseCase = new HashMap<>();
        //Agents used car in BaseCase now switched and got the MobilityBudget (amount = 100.0)
        scoreStatsFromBaseCase.put(Id.createPersonId("113ecc"),129.086211440912123);
        //Agent stays at home the whole day so doesn´t use his car so does not get the MobilityBudget
        scoreStatsFromBaseCase.put(Id.createPersonId("113efb"), 0.0);
        //Agent used car in BaseCase and is still using it --> no MobilityBudget
        scoreStatsFromBaseCase.put(Id.createPersonId("113f00"),46.33243742580156);
        //Agent didn´t use car in Base Case
        scoreStatsFromBaseCase.put(Id.createPersonId("113f02"),117.49532306296163);
        //Agent with commercial activiets are excluded from the MobilityBudget
        scoreStatsFromBaseCase.put(Id.createPersonId("commercial_820440"),113.84199583743978);
        //Agent didn´t use car in Base Case
        scoreStatsFromBaseCase.put(Id.createPersonId("113f00_ptCopy"), 47.73522118258644);

        for (Person p: persons.values()) {
            Assert.assertEquals(scoreStatsFromBaseCase.get(p.getId()), p.getSelectedPlan().getScore(), 0);
        }
    }

}