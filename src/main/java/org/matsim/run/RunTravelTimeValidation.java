package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.here.HereAPITravelTimeValidation;
import org.matsim.analysis.here.HereAPITravelTimeValidationConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManager;

import java.io.IOException;
import java.text.ParseException;

import static org.matsim.run.RunBaseCaseHamburgScenario.prepareConfig;
import static org.matsim.run.RunBaseCaseHamburgScenario.prepareScenario;

/**
 * @author zmeng
 */
public class RunTravelTimeValidation {
    private static final Logger log = Logger.getLogger(RunTravelTimeValidation.class);

    public static void main(String[] args) throws ParseException, IOException {

        args = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:HereAPITravelTimeValidation.date","2020-10-15",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey","EQ9BYtOQ-QKGBL2M2wR49hb6Aqxoa8yfkAbC77ZvQZg",
                "--config:HereAPITravelTimeValidation.useHereAPI","true",
                "--config:HereAPITravelTimeValidation.timeBin","0"
        };

        Config config = prepareConfig(args);
        config.plans().setInputFile("provide input plans");
        config.controler().setOutputDirectory("set output directory!");
        ConfigUtils.addOrGetModule(config, HereAPITravelTimeValidationConfigGroup.class).setNumOfTrips("1000");

        config.controler().setRunId("someRunId");

        Scenario scenario = prepareScenario(config);

        ParallelEventsManager eventManager = new ParallelEventsManager(false, 33554432);
        CarTripsExtractor carTripsExtractor = new CarTripsExtractor(scenario.getPopulation().getPersons().keySet(), scenario.getNetwork());
        eventManager.addHandler(carTripsExtractor);
        eventManager.initProcessing();
        (new MatsimEventsReader(eventManager)).readFile("provide output events file from simulation");

        HereAPITravelTimeValidation hereAPITravelTimeValidation = new HereAPITravelTimeValidation(carTripsExtractor,config);
        hereAPITravelTimeValidation.run();
    }
}
