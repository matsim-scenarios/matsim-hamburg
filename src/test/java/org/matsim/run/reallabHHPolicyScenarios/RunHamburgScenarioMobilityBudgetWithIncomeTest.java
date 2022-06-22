package org.matsim.run.reallabHHPolicyScenarios;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class RunHamburgScenarioMobilityBudgetWithIncomeTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void runTest() throws IOException, ParseException {

        String[] args = new String[]{
                "test/input//test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "4",
                "--config:controler.runId" , "RunBaseCaseHamburgScenarioIT",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "plans/test-hamburg.plans.xml",
        };


        RunBaseCaseWithMobilityBudget runner = new RunBaseCaseWithMobilityBudget(10., 10.5, null, false, 1.0);
        Config config = runner.prepareConfig(args);

        //adjusting strategy setting of config so agents try out different modes
        for (StrategyConfigGroup.StrategySettings setting:    config.strategy().getStrategySettings()) {
            if (setting.getStrategyName().equals("SubtourModeChoice")) {
                setting.setWeight(1.0);
            }
        }
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        Scenario scenario = runner.prepareScenario(config);

        scenario.getPopulation().getPersons().remove(Id.createPersonId("113f02_2violatingSubtours"));

        Controler controler = runner.prepareControler(scenario);

        MobilityBudgetTestListener handler = new MobilityBudgetTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });

        controler.run();

        Assert.assertEquals("wrong number of expected mobility budget payments!",1, handler.mobilityBudgetEvents.size());
        Assert.assertEquals("wrong person", "113ecc", handler.mobilityBudgetEvents.iterator().next().getPersonId().toString() );
        Assert.assertEquals("wrong mobility budget amount", 131.25 , handler.mobilityBudgetEvents.iterator().next().getAmount(), 0. );

        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
        HashMap<Id<Person>, Double> expectedScores = new HashMap<>();
        //Agents used car in BaseCase now switched and got the MobilityBudget (amount = ((4500*10.5)/30)/12=131.25). Score in base case = 115.10447635274537
//        expectedScores.put(Id.createPersonId("113ecc"), 160.3362114409121);
        expectedScores.put(Id.createPersonId("113ecc"), 160.3362114409121);
        //Agent stays at home the whole day so doesn´t use his car so does not get the MobilityBudget
        expectedScores.put(Id.createPersonId("113efb"), 0.0);
        //Agent used car in BaseCase and is still using it --> no MobilityBudget
        expectedScores.put(Id.createPersonId("113f00"), 46.27816254680561);
        //Agent didn´t use car in Base Case
        expectedScores.put(Id.createPersonId("113f02"), 117.49532306296163);
        //Agent didn´t use car in Base Case
        expectedScores.put(Id.createPersonId("113f02_2violatingSubtours"),109.85392359704746);
        //Agent with commercial activity are excluded from the MobilityBudget
        expectedScores.put(Id.createPersonId("commercial_820440"), 113.91685874278578);
        //Agent didn´t use car in Base Case
        expectedScores.put(Id.createPersonId("113f00_ptCopy"), 47.73522118258644);

        for (Person p : persons.values()) {
            Assert.assertEquals("expected other score for agent " + p.getId(), expectedScores.get(p.getId()), p.getSelectedPlan().getScore() , 0.);
        }

    }


}
