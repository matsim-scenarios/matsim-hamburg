package org.matsim.analysis;


import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author zmeng
 */
public class PlanBasedTripsFileWriter {

    private final Population population;
    private final Config config;

    public static void main(String[] args) {

        String populationsFile;
        String outputDirectory;
        String runId;

        if(args.length == 0){
            populationsFile = "";
            outputDirectory = "";
            runId = "";
        } else {
            populationsFile = args[0];
            outputDirectory = args[1];
            runId = args[2];
        }

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(populationsFile);
        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDirectory);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        PlanBasedTripsFileWriter planBasedTripsFileWriter = new PlanBasedTripsFileWriter(population, config);
        planBasedTripsFileWriter.write();
    }

    @Inject
    public PlanBasedTripsFileWriter(Population population, Config config) {
        this.population = population;
        this.config = config;
    }

    public void write(){
        String split = ";";
        String outputFolder = this.config.controler().getOutputDirectory();

        if(!outputFolder.endsWith("/")){
            outputFolder = outputFolder + "/";
        }

        String fileName = ".output_trips_from_plans.csv.gz";

        try {
            BufferedWriter writer = new BufferedWriter( new FileWriter(outputFolder + config.controler().getRunId() + fileName));
            writer.write("person" + split + "trip_number" + split + "trip_id" + split + "main_mode");

            for (Person person : population.getPersons().values()) {
                Plan plan = person.getSelectedPlan();
                var personId = person.getId();

                var trips = TripStructureUtils.getTrips(plan);
                int i = 1;
                for (TripStructureUtils.Trip trip : trips) {
                    writer.newLine();
                    writer.write(personId + split + i + split + personId + "_" + i + split + TripStructureUtils.identifyMainMode(trip.getTripElements()));
                    i++;
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
