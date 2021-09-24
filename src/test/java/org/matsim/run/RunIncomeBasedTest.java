package org.matsim.run;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

/**
 * @author zmeng
 */

//ignoring this test, as income based scoring is default (since a while) and this test does not really test something meaningful! (or in a meaningful way)!
@Ignore
public class RunIncomeBasedTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void incomeBasedTest() throws IOException {
        String[] args = new String[]{
                "test/input/test-hamburg.config.xml",
                "--config:controler.lastIteration" , "5"
        };

        Config config = prepareConfig(args);

        config.controler().setRunId("incomeBasedTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        config.subtourModeChoice().setConsiderCarAvailability(true);

        ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class).setUsePersonIncomeBasedScoring(true);

        Scenario scenario = prepareScenario(config);
        double income = 1000;
        for (Person person : scenario.getPopulation().getPersons().values()) {
            person.getAttributes().putAttribute("income", income);
            income*=10;
        }

        Controler controler = prepareControler(scenario);

        controler.run();
        //TODO do something more meaningful here! tschlenther aug '21
        Assert.assertNotEquals("after the income based approach a different score should be obtained",68.3508985518222,controler.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.executed).get(5));
    }
}
