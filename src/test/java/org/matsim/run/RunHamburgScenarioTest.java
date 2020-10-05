package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

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

        Config config = RunBaseCaseHamburgScenario.prepareConfig(args);

        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);

        controler.run();
    }

    @Test
    public void u17CarUsingTest(){

        String args[] = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "3"
        };

        Config config = RunBaseCaseHamburgScenario.prepareConfig(args);

        config.controler().setRunId("u17CarUsingTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        config.plans().setInputFile("test-u17-hamburg.plans.xml");
        config.subtourModeChoice().setConsiderCarAvailability(true);

        config.strategy().getStrategySettings().forEach(strategySettings ->
            {if(strategySettings.getStrategyName().equals("SubtourModeChoice"))
                strategySettings.setWeight(100.);
            });

        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);

        controler.run();

        ModeStatsControlerListener modeStatsControlerListener = controler.getInjector().getInstance(ModeStatsControlerListener.class);

        Assert.assertFalse("U17 person should not drive car",modeStatsControlerListener.getModeHistories().containsKey("car"));

    }
}