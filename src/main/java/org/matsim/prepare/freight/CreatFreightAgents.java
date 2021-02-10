package org.matsim.prepare.freight;

import com.sun.jdi.connect.Transport;
import org.jboss.logging.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author zmeng
 */
public class CreatFreightAgents {

    private static final Logger log = Logger.getLogger(CreatFreightAgents.class);
    private static final Random rnd = MatsimRandom.getLocalInstance();
    private static int totalTripNum = 0;
    private final String OUTPUT;
    private final Map<String, SimpleFeature> simpleFeatures = new HashMap<>();
    private final String SOURCE_DATA;
    private String[] head;
    private Scenario scenario;


    public CreatFreightAgents(String SOURCE_DATA, String shapeFile, String OUTPUT) throws IOException {

        Collection<SimpleFeature> allFeatures = ShapeFileReader.getAllFeatures(shapeFile);

        for (SimpleFeature feature : allFeatures) {
            simpleFeatures.put(feature.getAttribute("NO").toString(),feature);
        }
        this.SOURCE_DATA = SOURCE_DATA;
        this.OUTPUT = OUTPUT;

    }

    public static void main(String[] args) throws IOException {

        final String sourceData = "/Users/meng/shared-svn/projects/realLabHH/data/BVM_Modelldaten/UmlegungsmatrizenAnalyse2018_convert/all.csv";
        final String shapeFile = "/Users/meng/shared-svn/projects/realLabHH/data/BVM_Modelldaten/Verkehrszellen/Verkehrszellen_zone.SHP";
        final String outputPopulation = "output/";

        CreatFreightAgents creatFreightAgents = new CreatFreightAgents(sourceData,shapeFile,outputPopulation);
        creatFreightAgents.run();



    }

    private void run() throws IOException {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().plans().setInputCRS("EPSG:25832");
        Population population = scenario.getPopulation();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SOURCE_DATA));
            String line = reader.readLine();
            head = line.split(",");
            line = reader.readLine();
            while (line != null){
                // read source data line by line
                processLine(line,population);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Files.createDirectories(Path.of(OUTPUT));
        // downscale to 25%
        PopulationUtils.sampleDown(population, 0.25);
        PopulationUtils.writePopulation(population,OUTPUT + "hamburg-freight-25pct.plans.xml.gz");

        // downscale to 10%
        PopulationUtils.sampleDown(population, 0.4);
        PopulationUtils.writePopulation(population,OUTPUT + "hamburg-freight-10pct.plans.xml.gz");

        // downscale to 1%
        PopulationUtils.sampleDown(population, 0.1);
        PopulationUtils.writePopulation(population,OUTPUT + "hamburg-freight-1pct.plans.xml.gz");
    }

    private void processLine(String line, Population population) throws Exception {
        String[] data = line.split(",");
        /**
         * data[0] --> origin
         * data[1] --> destination
         * data[2] --> Lfw_Tag : Lfw is the type that do not have temporal distribution
         */
        for (int i = 2; i < data.length; i++) {
            Double[] timeSlots = getTimeSlots(head[i]);

            if (i==2 || !Arrays.equals(timeSlots, new Double[]{0., 24 * 3600.})) {

                if(this.simpleFeatures.containsKey(data[0]) && this.simpleFeatures.containsKey(data[1]))
                    createFreightPerson(data[0],data[1],data[i],timeSlots,getVehicleType(head[i]),population);
            }

        }
    }

    private void createFreightPerson(String origin, String destination, String num, Double[] timeSlots, String vehicleType, Population population) throws Exception {
        int tripNum = calTripNum(num);

        if(tripNum > 0 ){
            log.info("create " + tripNum + " " + vehicleType + " freight trip from origin " + origin + " to destination " + destination + " during timeslots [" + timeSlots[0] + "," + timeSlots[1] + "]");

            for (int i = 0; i < tripNum; i++) {
                totalTripNum++;

                Person pers = population.getFactory().createPerson(Id.create("freight_" + totalTripNum, Person.class));

                Plan plan = population.getFactory().createPlan();
                Activity startActivity = population.getFactory().createActivityFromCoord("freight", getRandomCoord(origin));
                startActivity.setEndTime(getRandomTime(timeSlots));
                plan.addActivity(startActivity);

                Leg leg;
                if(vehicleType.contains("OV"))
                    leg = population.getFactory().createLeg(TransportMode.pt);
                else
                    leg = population.getFactory().createLeg("freight_" + vehicleType);

                plan.addLeg(leg);

                Activity endActivity = population.getFactory().createActivityFromCoord("freight",getRandomCoord(destination));
                plan.addActivity(endActivity);

                pers.addPlan(plan);
                population.addPerson(pers);

                scenario.getPopulation().getPersons().get(pers.getId()).getAttributes().putAttribute(scenario.getConfig().plans().getSubpopulationAttributeName(), "freight");

                log.info("person: " + pers.getId() + " time" + startActivity.getEndTime());
            }
        }
    }

    private double getRandomTime(Double[] timeSlots) {
        return rnd.nextDouble() * (timeSlots[1] - timeSlots[0]) + timeSlots[0];
    }

    private Coord getRandomCoord(String zone) throws Exception {

            var feature = this.simpleFeatures.get(zone);
            double x = feature.getBounds().getMinX() + rnd.nextDouble() * (feature.getBounds().getMaxX() - feature.getBounds().getMinX());
            double y = feature.getBounds().getMinY() + rnd.nextDouble() * (feature.getBounds().getMaxY() - feature.getBounds().getMinY());
            Point p = MGC.xy2Point(x, y);
            return MGC.point2Coord(p);
    }

    private int calTripNum(String num) {
        int i = 0;
        if(num.contains(".")){
            String[] split = num.split("\\.");
            if(rnd.nextDouble() < Double.parseDouble("0." + split[1])){
                i = Integer.parseInt(split[0]) + 1;
            }else
                i = Integer.parseInt(split[0]);
        }else
            i = Integer.parseInt(num);
        return i;
    }

    private String getVehicleType(String colName){
        String[] colSplit = colName.split("_");
        return colSplit[colSplit.length-2];
    }

    private Double[] getTimeSlots(String colName){

        String[] colSplit = colName.split("_");
        switch (colSplit[colSplit.length-1]){

            case "Vorm":
                return new Double[]{6*3600.,10*3600.};
            case "Nachm":
                return new Double[]{15*3600.,19*3600.};
            case "Nacht":
                if (rnd.nextDouble() < 0.25)
                    return new Double[]{22*3600.,24*3600.};
                else
                    return new Double[]{0., 6*3600.};
            case "Rest":
                if (rnd.nextDouble() < 3./8.)
                    return new Double[]{19*3600., 22*3600.};
                else
                    return new Double[]{10*3600.,15*3600.};
            default:
                return new Double[]{0.,24*3600.};
        }
    }
}
