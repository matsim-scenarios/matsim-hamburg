package org.matsim.run.reallabHHPolicyScenarios;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Gryb
 */
//TODO can we speed up this test? For example, by setting lastIteration = 2...
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
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "plans/test-hamburg.plans.xml",
        };


        double dailyMobilityBudget = 100.0;

        RunBaseCaseWithMobilityBudget runner = new RunBaseCaseWithMobilityBudget(dailyMobilityBudget, -1., null, false, 1.0);
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

        PersonMoneyEvent event = handler.mobilityBudgetEvents.iterator().next();
        Assert.assertEquals("wrong person", "113f00_ptCopy", event.getPersonId().toString() );
        Assert.assertEquals("wrong mobility budget amount", dailyMobilityBudget , event.getAmount(), 0. );

        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
        HashMap<Id<Person>, Double> expectedScores = new HashMap<>();
        //Agents used car in BaseCase and still uses it
        expectedScores.put(Id.createPersonId("113ecc"),121.64330939529705);
        //Agent stays at home the whole day so doesn´t use his car so does not get the MobilityBudget
        expectedScores.put(Id.createPersonId("113efb"), 0.0);
        //Agent used car in BaseCase and still uses it
        expectedScores.put(Id.createPersonId("113f00"),30.318758225233882);
        //Agent didn´t use car in Base Case
        expectedScores.put(Id.createPersonId("113f02"),117.12172395135431);
        //Agent with commercial activities are excluded from the MobilityBudget
        expectedScores.put(Id.createPersonId("commercial_820440"),113.91685874278578);
        //Agent used car in Base Case and now switched to all bike --> MobilityBudget and same score as the copy
        expectedScores.put(Id.createPersonId("113f00_ptCopy"), 84.83619800878152);

        for (Person p: persons.values()) {
            Assert.assertEquals("expected other score for agent " + p.getId(), expectedScores.get(p.getId()), p.getSelectedPlan().getScore() , 0.);
        }
    }

}

class MobilityBudgetTestListener implements PersonMoneyEventHandler{

    Set<PersonMoneyEvent> mobilityBudgetEvents = new HashSet<>();

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        // MobilityBudgetEventHandler also throws events for each person that does not get any mobilityBudget
        if (event.getPurpose().equals("mobilityBudget") && event.getAmount() > 0.) this.mobilityBudgetEvents.add(event);
    }

    @Override
    public void reset(int iteration) {
        PersonMoneyEventHandler.super.reset(iteration);
        this.mobilityBudgetEvents.clear();
    }
}