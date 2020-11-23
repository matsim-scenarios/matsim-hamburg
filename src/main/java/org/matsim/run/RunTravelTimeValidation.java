package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsRouteValidator;
import org.matsim.contrib.analysis.vsp.traveltimedistance.TravelTimeValidationRunner;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.matsim.run.RunBaseCaseHamburgScenario.prepareConfig;

/**
 * @author zmeng
 */
public class RunTravelTimeValidation {
    private static final Logger log = Logger.getLogger(RunTravelTimeValidation.class);

    public static void main(String[] args) throws ParseException {

        args = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:HereAPITravelTimeValidation.date","2019-06-13",
                "--config:HereAPITravelTimeValidation.HereMapsAPIKey","EQ9BYtOQ-QKGBL2M2wR49hb6Aqxoa8yfkAbC77ZvQZg",
                "--config:HereAPITravelTimeValidation.useHereAPI","true",
                "--config:HereAPITravelTimeValidation.timeWindow","08:00:00-10:00:00"
        };

        Config config = prepareConfig(args);
        config.plans().setInputFile("/Volumes/Macintosh HD/Users/meng/work/realLabHH/calibrate/HERE/hh-1pct-18-0.output_plans.xml.gz");
        config.controler().setOutputDirectory("/Users/meng/work/realLabHH/calibrate/HERE");
        runHEREValidation(config, "/Users/meng/work/realLabHH/calibrate/HERE" + "/hh-1pct-18-7.output_events.xml.gz");

    }

    public static  void runHEREValidation(Config config, String events) throws ParseException {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        HereAPITravelTimeValidationConfigGroup hereAPITravelTimeValidationConfigGroup = ConfigUtils.addOrGetModule(config, HereAPITravelTimeValidationConfigGroup.class);

        if(hereAPITravelTimeValidationConfigGroup.isUseHereAPI()){

            TravelTimeValidationRunner runner;
            final var populationIds = scenario.getPopulation().getPersons().keySet();
            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(config.global().getCoordinateSystem(), TransformationFactory.WGS84);

            String[] timeWindowString = hereAPITravelTimeValidationConfigGroup.getTimeWindow().split("-");

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            double reference = (dateFormat.parse("00:00:00")).getTime();

            double time1 = (dateFormat.parse(timeWindowString[0]).getTime() - reference) / 1000L;
            double time2 = (dateFormat.parse(timeWindowString[1]).getTime() - reference) / 1000L;

            Tuple<Double, Double> timeWindow = new Tuple(time1,time2);

            var outputfolder = config.controler().getOutputDirectory() + "/" + "here_validation_" + config.controler().getRunId() + "_" + hereAPITravelTimeValidationConfigGroup.getDate() + "_" + hereAPITravelTimeValidationConfigGroup.getTimeWindow() + "/";
            HereMapsRouteValidator validator = new HereMapsRouteValidator(outputfolder, hereAPITravelTimeValidationConfigGroup.getHereMapsAPIKey(), hereAPITravelTimeValidationConfigGroup.getDate(), transformation);

            if (hereAPITravelTimeValidationConfigGroup.getNumOfTrips().equals("all")){
                runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events, outputfolder, validator, (int) Double.POSITIVE_INFINITY, timeWindow);
            }
            else  {
                runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events, outputfolder, validator, Integer.valueOf(hereAPITravelTimeValidationConfigGroup.getNumOfTrips()), timeWindow);
            }
            //Setting this to true will write out the raw JSON files for each calculated route
            validator.setWriteDetailedFiles(false);
            runner.run();

        }
    }

    public static void runHEREValidation(Controler controler, String events) throws ParseException {
        runHEREValidation(controler.getConfig(),events);
    }

    public static void runHEREValidation(Controler controler) throws ParseException {
        runHEREValidation(controler, controler.getConfig().controler().getOutputDirectory() + "/" + controler.getConfig().controler().getRunId() + ".output_events.xml.gz");
    }
}
