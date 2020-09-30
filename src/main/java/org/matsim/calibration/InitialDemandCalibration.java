package org.matsim.calibration;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunHamburgScenario;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * calibrate the characteristic value in initial demands:
 *  The area covered by the population file
 *  The number of residents in each area
 *  avg.trips number per resident in each area
 *  avg.trip distance per resident in each area
 *  median of trip distance per destination-activity
 *  distribution of destination-activity in each area
 * @author zmeng
 */
public class InitialDemandCalibration {
    private Scenario scenario;
    private Collection<SimpleFeature> features;

    public InitialDemandCalibration(String initialDemand, String shapeFile) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(RunHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputCRS(RunHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputFile(initialDemand);
        this.scenario = ScenarioUtils.loadScenario(config);
        if(shapeFile != null)
            this.features = ShapeFileReader.getAllFeatures(shapeFile);
    }

    public static void main(String[] args) throws Exception {
        //todo: upload to svn
        InitialDemandCalibration initialDemandCalibration = new InitialDemandCalibration("/Users/meng/IdeaProjects/matsim-hamburg/scenarios/input/hamburg-v1.0-25pct.plans.xml.gz",
                "/Users/meng/work/realLabHH/files/hamburg/hamburg_HVV/hamburg_HVV.shp");
        String outputPath = "/Users/meng/IdeaProjects/matsim-hamburg/scenarios/initialDemandCalibration/";
        printMap(initialDemandCalibration.getArea2numOfResidents("name"),new String[]{"area","count"},outputPath+"area2numOfResidents.csv",",");

    }

    public Scenario getScenario() {
        return scenario;
    }


    /**
     * This method analyze the number of residents living in each areas
     */
    public Map<String,Long> getArea2numOfResidents(String areaType) throws Exception {
        Map<String,List<Geometry>> areaId2Geometries = new HashMap<>();
        Map<String,Long> area2numOfResidents = new HashMap<>();
        for (SimpleFeature feature :
                features) {
            String id = feature.getAttribute(areaType).toString();
            Geometry geometry = (Geometry)feature.getDefaultGeometry();

            if(!areaId2Geometries.containsKey(id))
                areaId2Geometries.put(id,new LinkedList<>());

              areaId2Geometries.get(id).add(geometry);
        }

        Map<String,Set<Person>> homeArea2Persons = distributeHomeArea2Persons(areaId2Geometries);
        for (String id :
                homeArea2Persons.keySet()) {
            long count = homeArea2Persons.get(id).stream().count();
            area2numOfResidents.put(id,count);
        }
        return area2numOfResidents;
    }

    private  Map<String,Set<Person>> distributeHomeArea2Persons(Map<String, List<Geometry>> areaId2Geometries) throws Exception {
        Map<Person,Geometry> person2homeLocation = new HashMap<>();
        Map<String,Set<Person>> area2Persons = new HashMap<>();
        for (Person person :
                scenario.getPopulation().getPersons().values()){
            Plan selectedPlan = person.getSelectedPlan();
            for (PlanElement pe :
                    selectedPlan.getPlanElements()) {
                if (pe instanceof Activity){
                    Activity homeActivity = (Activity) pe;
                    if (homeActivity.getType().contains("home"))
                        person2homeLocation.put(person, MGC.coord2Point(homeActivity.getCoord()));
                    break;
                }
            }
        }
        for (Person person :
                person2homeLocation.keySet()) {
            for (String id :
                    areaId2Geometries.keySet()) {
                for (Geometry area :
                        areaId2Geometries.get(id)) {
                    if (area.contains(person2homeLocation.get(person))){
                        if (!area2Persons.containsKey(id)){
                            area2Persons.put(id, new HashSet<>());
                        }
                        area2Persons.get(id).add(person);
                        break;
                    }
                }
            }
        }
        return area2Persons;
    }

    /**
     * this method analyze the area covered by initial demand
     * @return Coord[minx, miny] and Coord[maxx, maxy]
     */
    public Coord[] getDemandAreaBoundaryCoord(){
        Coord[] boundary = new Coord[2];
        Set<Double> xSet = new LinkedHashSet<>();
        Set<Double> ySet = new LinkedHashSet<>();

        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            for (Plan plan :
                    person.getPlans()) {
                for (PlanElement planElement:
                        plan.getPlanElements()) {

                    if(!(planElement instanceof Activity))
                        continue;

                    Activity activity = (Activity) planElement;
                    xSet.add(activity.getCoord().getX());
                    ySet.add(activity.getCoord().getY());
                }
            }
        }
        boundary[0] = new Coord(Collections.min(xSet),Collections.min(ySet));
        boundary[1] = new Coord(Collections.max(xSet),Collections.max(ySet));
        return boundary;
    }

    public Set<String> getActivityTypes(){
        Set<String> activityTypes = new HashSet<>();
        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            for (Plan plan :
                    person.getPlans()) {
                for (PlanElement planElement:
                        plan.getPlanElements()) {

                    if(!(planElement instanceof Activity))
                        continue;

                    Activity activity = (Activity) planElement;

                    activityTypes.add(activity.getType());
                }
            }
        }
        return activityTypes;
    }


    private static void printMap(Map results, String[] head, String file, String splitSymbol) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(fileWriter);
        bw.write(head2String(head,splitSymbol));

        for (Object key :
                results.keySet()) {
            bw.newLine();
            bw.write(key + splitSymbol);
            bw.write(results.get(key).toString());
        }

        bw.close();
    }

    private static String head2String(String[] head, String splitSymbol) {
        if (head == null || head.length <= 0)
            return null;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < head.length - 1; i++) {
            builder.append(head[i]);
            builder.append(splitSymbol);
        }
        builder.append(head[head.length - 1]);
        return builder.toString();
    }
}
