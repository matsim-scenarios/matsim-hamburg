package org.matsim.analysis;


import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

/**
 * @author zmeng
 */
public class PlanBasedTripsFileWriter {

    private final Population population;
    private final Config config;
    private boolean calcDistances = false;

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
        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputFile(populationsFile);
        config.controler().setRunId(runId);
        config.controler().setOutputDirectory(outputDirectory);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        PlanBasedTripsFileWriter planBasedTripsFileWriter = new PlanBasedTripsFileWriter(population, config);
        planBasedTripsFileWriter.setCalcDistances(true);
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
            FileOutputStream output = new FileOutputStream(outputFolder + config.controler().getRunId() + fileName);
            Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), "UTF-8");
            writer.write("person" + split + "trip_number" + split + "trip_id" + split + "main_mode");
            if(calcDistances) writer.write(split + "euclideanDistance");
            for (Person person : population.getPersons().values()) {
                Plan plan = person.getSelectedPlan();
                var personId = person.getId();

                var trips = TripStructureUtils.getTrips(plan);
                int i = 1;
                for (TripStructureUtils.Trip trip : trips) {
                    String line = personId + split + i + split + personId + "_" + i + split + TripStructureUtils.identifyMainMode(trip.getTripElements());
                    if(calcDistances){
                        line += split + DistanceUtils.calculateDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
                    }
                    writer.write('\n');
                    writer.write(line);
                    i++;
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setCalcDistances(boolean calcDistances) {
        this.calcDistances = calcDistances;
    }
}
