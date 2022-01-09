package org.matsim.prepare;

import static org.matsim.run.RunBaseCaseHamburgScenario.VERSION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author zmeng
 * @author gryb
 */
public class PreparePopulationMobilityBudget {
    private static final Logger log = Logger.getLogger(PreparePopulationMobilityBudget.class);

    private final Scenario scenario;
    private final Path output;
    private final String personIncomeFile;

    public PreparePopulationMobilityBudget(String initialDemand,String attributes, String person2incomeFile, Path output) {

        Config config = ConfigUtils.createConfig();

        config.plans().setInputPersonAttributeFile(attributes);
        config.plans().setInputFile(initialDemand);

        config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);

        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputCRS("EPSG:25832");

        scenario = ScenarioUtils.loadScenario(config);


        this.personIncomeFile = person2incomeFile;

        this.output = output;
    }

    public static void main(String[] args){

        // population files can not be public, thus they are stored privately in svn, to get the access of those folders please contact us in github

//        String initialDemand = "../shared-svn/projects/realLabHH/matsim-input-files/v1/optimizedPopulation.xml.gz";
//        String attributes = "../shared-svn/projects/realLabHH/matsim-input-files/v1/additionalPersonAttributes.xml.gz";
//        String personIncomeFile = "../shared-svn/projects/matsim-hamburg/hamburg-v1.0/person_specific_info/person2income.csv";
//        String outputPath = "../shared-svn/projects/matsim-hamburg/hamburg-v1.0/";
//
//        PreparePopulation preparePopulation = new PreparePopulation(initialDemand, attributes, personIncomeFile,Path.of(outputPath));
//        preparePopulation.run();

        //
        String tem = "tem";
        String dir = args[1];
        Path output = Path.of(dir + "/shared-svn/projects/matsim-hamburg/hamburg-v1.0/");
        String populationsFile = args[0];

        Config config = ConfigUtils.createConfig();

        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputCRS("EPSG:25832");

        config.plans().setInputFile(populationsFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-" + tem + "-25pct.plans.xml.gz").toString());
        // sample 25% to 10%
        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.4);
        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-" + tem + "-10pct.plans.xml.gz").toString());

        // sample 10% to 1%
        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.1);
        PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-" + tem + "-1pct.plans.xml.gz").toString());

    }

    public void run() throws IOException {
        var person2Income = this.readPersonId2Income(this.personIncomeFile);

        for (Person person : scenario.getPopulation().getPersons().values()) {

            person.getAttributes().putAttribute("subpopulation", "person");
            addPersonIncomeFromCSVFile(person, person2Income);

            // set CarAvail of person under 18 never, set always otherwise
            if(Integer.parseInt(person.getAttributes().getAttribute("age").toString()) < 18)
                PersonUtils.setCarAvail(person, "never");
            else
                PersonUtils.setCarAvail(person, "always");
            // set Income for person


            // remove attributes that are confusing and we will not need
            person.getAttributes().removeAttribute("sim_carAvailability");
            person.getAttributes().removeAttribute("sim_ptAbo");

            
            createPersonMobilityBudget(person);

            for (Plan plan : person.getPlans()) {
                Plan newPlan = preparePlan(plan);
                person.removePlan(plan);
                person.addPlan(newPlan);
            }
        }

        Files.createDirectories(output);
        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-25pct.plans.xml.gz").toString());

        // sample 25% to 10%
        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.4);
        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-10pct.plans.xml.gz").toString());

        // sample 10% to 1%
        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.1);
        PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-1pct.plans.xml.gz").toString());

    }

    private Plan preparePlan(Plan plan) {
        int activityCounter = 0;

        RoutingModeMainModeIdentifier mainModeIdentifier = new RoutingModeMainModeIdentifier();

        String initialStartTimesString = (String) plan.getPerson().getAttributes().getAttribute("IPD_actStartTimes");
        String initialEndTimesString = (String) plan.getPerson().getAttributes().getAttribute("IPD_actEndTimes");

        String[] initialStartTimes = initialStartTimesString.split(";");
        String[] initialEndTimes = initialEndTimesString.split(";");

        Plan newPlan = scenario.getPopulation().getFactory().createPlan();

        Activity firstActivity = (Activity) plan.getPlanElements().get(0);
        firstActivity.setFacilityId(null);
        firstActivity.setLinkId(null);
        splitActivityTypesBasedOnDuration(firstActivity);
        firstActivity.getAttributes().putAttribute("initialStartTime", initialStartTimes[activityCounter]);
        firstActivity.getAttributes().putAttribute("initialEndTime", initialEndTimes[activityCounter]);

        newPlan.addActivity(firstActivity);

        for (Trip trip : TripStructureUtils.getTrips(plan.getPlanElements())) {
            activityCounter++;

            String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
            Leg leg = scenario.getPopulation().getFactory().createLeg(mainMode);
            newPlan.addLeg(leg);

            Activity destinationActivity = trip.getDestinationActivity();
            destinationActivity.setFacilityId(null);
            destinationActivity.setLinkId(null);
            splitActivityTypesBasedOnDuration(destinationActivity);

            destinationActivity.getAttributes().putAttribute("initialStartTime", initialStartTimes[activityCounter]);
            destinationActivity.getAttributes().putAttribute("initialEndTime", initialEndTimes[activityCounter]);

            newPlan.addActivity(destinationActivity);
        }

        mergeOvernightActivities(newPlan);
        return newPlan;
    }

    /**
     * Split activities into typical durations to improve value of travel time savings calculation.
     *
     * @see playground.vsp.openberlinscenario.planmodification.CemdapPopulationTools
     */
    private void splitActivityTypesBasedOnDuration(Activity act) {

        final double timeBinSize_s = 600.;


        double duration = act.getEndTime().orElse(24 * 3600)
                - act.getStartTime().orElse(0);

        int durationCategoryNr = (int) Math.round((duration / timeBinSize_s));

        if (durationCategoryNr <= 0) {
            durationCategoryNr = 1;
        }

        String newType = act.getType() + "_" + (durationCategoryNr * timeBinSize_s);
        act.setType(newType);

    }

    /**
     * See {@link playground.vsp.openberlinscenario.planmodification.CemdapPopulationTools}.
     */
    private void mergeOvernightActivities(Plan plan) {

        if (plan.getPlanElements().size() > 1) {
            Activity firstActivity = (Activity) plan.getPlanElements().get(0);
            Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

            String firstBaseActivity = firstActivity.getType().split("_")[0];
            String lastBaseActivity = lastActivity.getType().split("_")[0];

            if (firstBaseActivity.equals(lastBaseActivity)) {
                double mergedDuration = Double.parseDouble(firstActivity.getType().split("_")[1]) + Double.parseDouble(lastActivity.getType().split("_")[1]);


                firstActivity.setType(firstBaseActivity + "_" + mergedDuration);
                lastActivity.setType(lastBaseActivity + "_" + mergedDuration);
            }
        }  // skipping plans with just one activity

    }

    private void addPersonIncomeFromCSVFile(Person person, Map<String,Double> personId2Income) {

        final Random rnd = MatsimRandom.getLocalInstance();

        if(personId2Income.containsKey(person.getId().toString())){
            person.getAttributes().putAttribute("income", personId2Income.get(person.getId().toString()));
        } else {
            double income = 0.;
            double rndDouble = rnd.nextDouble();
            if (rndDouble <= 0.1) income = 826.;
            else if (rndDouble > 0.1 && rndDouble <= 0.2) income = 1142.;
            else if (rndDouble > 0.2 && rndDouble <= 0.3) income = 1399.;
            else if (rndDouble > 0.3 && rndDouble <= 0.4) income = 1630.;
            else if (rndDouble > 0.4 && rndDouble <= 0.5) income = 1847.;
            else if (rndDouble > 0.5 && rndDouble <= 0.6) income = 2070.;
            else if (rndDouble > 0.6 && rndDouble <= 0.7) income = 2332.;
            else if (rndDouble > 0.7 && rndDouble <= 0.8) income = 2659.;
            else if (rndDouble > 0.8 && rndDouble <= 0.9) income = 3156.;
            else if (rndDouble > 0.9) income = 4329.;
            else {
                throw new RuntimeException("Aborting..." + rndDouble);
            }
            person.getAttributes().putAttribute("income", income);
        }
    }

    private Map<String, Double> readPersonId2Income(String person2incomeFile) throws IOException {
        Map<String, Double> personId2Income = new HashMap<>();
        File file = new File(person2incomeFile);
        log.info("read personId2incomeFile from" + file.getAbsolutePath());

        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String firstLine = csvReader.readLine();
        while ((firstLine = csvReader.readLine()) != null) {
            String[] income = firstLine.split(",");
            personId2Income.put(income[0],Double.valueOf(income[1]));
        }
        csvReader.close();
        return personId2Income;
    }

    private Person createPersonMobilityBudget (Person person) {

        double random = Math.random();
        // not every person gets the mobilityBudgetAttribute
        if (random <= 0.10) {
            // the monetary value of the mobilityBudget is depended on the income of the agent
            double mobilityBudget = (double) person.getAttributes().getAttribute("income") * 0.10;
            person.getAttributes().putAttribute("mobilityBudget", mobilityBudget);
        }



        return person;
    }
}
