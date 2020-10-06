package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.hamburg.replanning.modules.HamburgSubtourModeChoicePlanStrategy;
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
    public void u17CarUsingTest(){
        String args[] = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "10"
        };

        Config config = prepareConfig(args);

        config.controler().setRunId("u17CarUsingTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        config.plans().setInputFile("test-u17-hamburg.plans.xml");
        config.subtourModeChoice().setConsiderCarAvailability(false);

        config.strategy().getStrategySettings().forEach(strategySettings ->
        {if(strategySettings.getStrategyName().equals("SubtourModeChoice")){
            strategySettings.setStrategyName(HamburgSubtourModeChoicePlanStrategy.class.getCanonicalName());
            strategySettings.setWeight(100);
        } //else
            //strategySettings.setWeight(0);
        });

        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();

        ModeStatsControlerListener modeStatsControlerListener = controler.getInjector().getInstance(ModeStatsControlerListener.class);

        Assert.assertFalse("U17 person should not drive car",modeStatsControlerListener.getModeHistories().containsKey("car"));
    }
}