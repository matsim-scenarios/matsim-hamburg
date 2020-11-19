package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsRouteValidator;
import org.matsim.contrib.analysis.vsp.traveltimedistance.TravelTimeValidationRunner;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author zmeng
 */
public class RunTravelTimeValidation {
//    public static void main(String[] args) {
//        args = new String[]{
//                "/Users/meng/work/realLabHH/calibrate/plans/hh-1pct-17.output_plans.xml.gz", // plans
//                "/Users/meng/work/realLabHH/calibrate/events/hh-1pct-17.output_events.xml.gz", // events
//                "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1.0-1pct/input/hamburg-v1.0-network-with-pt.xml.gz", // network
//                "EPSG:25832", // epsg
//                "uH4B4BCIRbl9F-RiJ69H9a2IteJrt0dAoSSug8OuIIw", // apiKey
//                "scenarios/output/HEREValidation", // outputFolder
//                "2019-06-13", // date Wednesday
//                "500"
//        };
//        String plans = args[0];
//        String events = args[1];
//        String network = args [2];
//        String epsg = args[3];
//        String apiKey = args[4];
//        String outputfolder = args[5];
//        String date = args[6];
//        Integer tripsToValidate = null;
//        if (args.length>7){
//            tripsToValidate = Integer.parseInt(args[7]);
//        }
//
//        Set<Id<Person>> populationIds = new HashSet<>();
//        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//        new MatsimNetworkReader(scenario.getNetwork()).readFile(network);
//        StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
//        spr.addAlgorithm(new PersonAlgorithm() {
//            @Override
//            public void run(Person person) {
//                populationIds.add(person.getId());
//            }
//        });
//        spr.readFile(plans);
//        System.out.println("populationId Size is " + populationIds.size());
//
//
//        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(epsg, TransformationFactory.WGS84);
//        HereMapsRouteValidator validator = new HereMapsRouteValidator(outputfolder, apiKey, date, transformation);
//        //Setting this to true will write out the raw JSON files for each calculated route
//        validator.setWriteDetailedFiles(false);
//        TravelTimeValidationRunner runner;
//        if (tripsToValidate != null){
//            runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events, outputfolder, validator, tripsToValidate);
//        }
//        else  {
//            runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events, outputfolder, validator);
//        }
//        runner.run();
//    }

    public static void runHEREValidation(Controler controler) {
        Config config = controler.getConfig();
        Scenario scenario = controler.getScenario();
        HereAPITravelTimeValidationConfigGroup hereAPITravelTimeValidationConfigGroup = ConfigUtils.addOrGetModule(config, HereAPITravelTimeValidationConfigGroup.class);

        if(hereAPITravelTimeValidationConfigGroup.isUseHereAPI()){

            TravelTimeValidationRunner runner;
            final var populationIds = scenario.getPopulation().getPersons().keySet();
            final var events = config.controler().getOutputDirectory()  + config.controler().getRunId() + ".output_events.xml.gz";
            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(config.global().getCoordinateSystem(), TransformationFactory.WGS84);

            var outputfolder = config.controler().getOutputDirectory() + "/" + "here_validation_" + hereAPITravelTimeValidationConfigGroup.getDate() + "/";
            HereMapsRouteValidator validator = new HereMapsRouteValidator(outputfolder, hereAPITravelTimeValidationConfigGroup.getHereMapsAPIKey(), hereAPITravelTimeValidationConfigGroup.getDate(), transformation);

            if (hereAPITravelTimeValidationConfigGroup.getNumOfTrips().equals("all")){
                runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events, outputfolder, validator);
            }
            else  {
                runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events, outputfolder, validator);
            }
            //Setting this to true will write out the raw JSON files for each calculated route
            validator.setWriteDetailedFiles(false);
            runner.run();

            }
        }

}
