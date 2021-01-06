package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ModeStatsControlerListener;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.analysis.here.HereAPIControlerListener;
import org.matsim.analysis.here.HereAPITravelTimeValidation;
import org.matsim.analysis.here.HereAPITravelTimeValidationConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

/**
 * @author zmeng
 */
public class RunHamburgScenarioTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
          "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "2",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.date","2019-06-13",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey","EQ9BYtOQ-QKGBL2M2wR49hb6Aqxoa8yfkAbC77ZvQZg",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:HereAPITravelTimeValidation.numOfTrips","5",
                "--config:HereAPITravelTimeValidation.timeBin","3600"

        };

        Config config = prepareConfig(args);

        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                CarTripsExtractor carTripsExtractor = new CarTripsExtractor(scenario.getPopulation().getPersons().keySet(), scenario.getNetwork());
                this.addEventHandlerBinding().toInstance(carTripsExtractor);
                this.addControlerListenerBinding().to(HereAPIControlerListener.class);
                this.bind(HereAPITravelTimeValidation.class).toInstance(new HereAPITravelTimeValidation(carTripsExtractor,config));
            }
        });
        controler.run();


    }
    
    @Test
    public void runTestParking() throws IOException {

        String args[] = new String[]{
          "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "10",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.date","2019-06-13",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey","EQ9BYtOQ-QKGBL2M2wR49hb6Aqxoa8yfkAbC77ZvQZg",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:HereAPITravelTimeValidation.numOfTrips","5",
                "--config:HereAPITravelTimeValidation.timeBin","3600"

        };

        Config config = prepareConfig(args);

        config.controler().setRunId("runTestParking");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        
        HamburgExperimentalConfigGroup hamburgConfig = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);
        hamburgConfig.setParkPressureLinkAttributeFile(null);
        
        Scenario scenario = prepareScenario(config);
        
        for (Link link : scenario.getNetwork().getLinks().values()) {
        	link.getAttributes().putAttribute("parkPressure", 300.);
        }
        
        Controler controler = prepareControler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                CarTripsExtractor carTripsExtractor = new CarTripsExtractor(scenario.getPopulation().getPersons().keySet(), scenario.getNetwork());
                this.addEventHandlerBinding().toInstance(carTripsExtractor);
                this.addControlerListenerBinding().to(HereAPIControlerListener.class);
                this.bind(HereAPITravelTimeValidation.class).toInstance(new HereAPITravelTimeValidation(carTripsExtractor,config));
            }
        });
        controler.run();


    }
}