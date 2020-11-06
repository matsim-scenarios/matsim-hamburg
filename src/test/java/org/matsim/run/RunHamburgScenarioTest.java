package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

/**
 * @author zmeng
 */
public class RunHamburgScenarioTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest(){

        String args[] = new String[]{
          "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "2"

        };

        Config config = prepareConfig(args);

        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);

        controler.run();
    }


    @Test
    public void u18CarUsingTest(){
        String args[] = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "10",
                "--config:planCalcScore.scoringParameters[subpopulation=null].modeParams[mode=car].constant", "-7"
        };

        Config config = prepareConfig(args);

        config.controler().setRunId("u18CarUsingTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        config.plans().setInputFile("test-u18-hamburg.plans.xml");
        config.subtourModeChoice().setConsiderCarAvailability(true);

        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();

        ModeStatsControlerListener modeStatsControlerListener = controler.getInjector().getInstance(ModeStatsControlerListener.class);

        Assert.assertFalse("U18 person should not drive car",modeStatsControlerListener.getModeHistories().containsKey("car"));
    }

    @Test
    public void incomeBasedScoringFunctionTest(){

        String args[] = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "0"
        };

        Config config = prepareConfig(args);

        config.controler().setRunId("incomeBasedScoringFunctionTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        config.plans().setInputFile("test-hamburg.plans.xml");
        config.subtourModeChoice().setConsiderCarAvailability(true);

        Scenario scenario = prepareScenario(config);

        for (Person person: scenario.getPopulation().getPersons().values()) {
            person.getAttributes().putAttribute("income",10000);
        }

        Controler controler = prepareControler(scenario);

        controler.run();


    }
}