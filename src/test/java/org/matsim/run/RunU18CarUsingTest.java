package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

/**
 * @author zmeng
 */
public class RunU18CarUsingTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void u18CarUsingTest() throws IOException {
        String[] args = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "5",
                "--config:planCalcScore.scoringParameters[subpopulation=null].modeParams[mode=car].constant", "-7"
        };

        Config config = prepareConfig(args);

        config.controler().setRunId("u18CarUsingTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        config.plans().setInputFile("plans/test-u18-hamburg.plans.xml");
        ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class).setUsePersonIncomeBasedScoring(false); //TODO assign (dummy) incomes and remove this config parameter! such that incomeBasedScoring is mandatory!
        config.subtourModeChoice().setConsiderCarAvailability(true);

        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();

        ModeStatsControlerListener modeStatsControlerListener = controler.getInjector().getInstance(ModeStatsControlerListener.class);

        Assert.assertFalse("U18 person should not drive car",modeStatsControlerListener.getModeHistories().containsKey("car"));
    }
}
