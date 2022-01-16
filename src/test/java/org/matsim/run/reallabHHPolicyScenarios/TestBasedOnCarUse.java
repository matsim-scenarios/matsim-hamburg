package org.matsim.run.reallabHHPolicyScenarios;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.prepare.SelectionMobilityBudget;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.text.ParseException;

/**
 * @author Gryb
 */
//TODO can we speed up this test? For example, by setting lastIteration = 2...
public class TestBasedOnCarUse {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException, ParseException {

        String[] args = new String[]{
                "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.2/input/baseCase/hamburg-v2.2-10pct.config.baseCase.xml" ,
                "--config:controler.lastIteration" , "4",
                "--config:controler.runId" , "RunBaseCaseHamburgScenarioIT",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "/Users/gregorr/Downloads/hamburg-v2.2-10pct.input-plans.xml.gz",
                "--config:hamburgExperimental.fixedDailyMobilityBudget","100.",
                "--config:hamburgExperimental.basedOnCarUse","true",
        };

        RunBaseCaseWithMobilityBudget runner = new RunBaseCaseWithMobilityBudget(1., -1., null, false, 1.0, true);
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
        //SelectionMobilityBudget.basedOnCarUse(scenario.getPopulation(), scenario.getConfig(), runner.personsEligibleForMobilityBudget);
        for (Id id: runner.personsEligibleForMobilityBudget.keySet()) {
            System.out.println(id.toString()+" " +runner.personsEligibleForMobilityBudget.get(id));
        }
        //controler.run();
    }

}

