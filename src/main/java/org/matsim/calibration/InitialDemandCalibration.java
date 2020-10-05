package org.matsim.calibration;

import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        config.global().setCoordinateSystem(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputCRS(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputFile(initialDemand);
        this.scenario = ScenarioUtils.loadScenario(config);
        if( shapeFile != null )
            this.features = this.getAllFeatures(shapeFile);
    }

    public static void main(String[] args) throws Exception {
        //todo: upload to svn
        InitialDemandCalibration initialDemandCalibration = new InitialDemandCalibration("/Users/meng/IdeaProjects/matsim-hamburg/scenarios/input/hamburg-v1.0-1pct.plans.xml.gz",
                "/Users/meng/work/realLabHH/files/hamburg/hamburg_HVV/hamburg_HVV.shp");
        String outputPath = "/Users/meng/IdeaProjects/matsim-hamburg/scenarios/initialDemandCalibration/";
        //printMap(initialDemandCalibration.getArea2numOfResidents("name"),new String[]{"area","count"},outputPath+"area2numOfResidents.csv",",");
        initialDemandCalibration.printAttributesStatistics(outputPath, new String[]{"IPD_actStartTimes","IPD_actEndTimes"});

    }

    public void printAttributesStatistics(String outputPath, String...excludeAttributesName) throws IOException {
        Population population = this.scenario.getPopulation();
        Set<String> attributes = getAttributes(population);

        for (String attributeName :
                attributes) {
            if(Arrays.asList(excludeAttributesName).contains(attributeName))
                continue;
            printMap(getAttributesStatistic(attributeName,population),new String[]{"name","count"},outputPath+attributeName+".csv",",");
        }
    }

    private Set<String> getAttributes(Population population){
        Set<String> attributes = new HashSet<>();

        for (Person person :
                population.getPersons().values()) {
            attributes.addAll(person.getAttributes().getAsMap().keySet());
        }
        return attributes;
    }

    private Map<String,Integer> getAttributesStatistic(String attribute, Population population){
        Map<String,Integer> attributesStatistic = new HashMap<>();

        for (Person p :
                population.getPersons().values()) {
            Object attributeValue;

            if(p.getAttributes().getAsMap().containsKey(attribute))
                attributeValue = p.getAttributes().getAttribute(attribute);
            else
                attributeValue = "noneExists";

            if(!attributesStatistic.containsKey(attributeValue.toString()))
                attributesStatistic.put(attributeValue.toString(),0);

            attributesStatistic.put(attributeValue.toString(),
                    attributesStatistic.get(attributeValue.toString()) + 1);
        }
        return  attributesStatistic;
    }




    /**
     * analyze the number of residents living in each areas
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
            long count = homeArea2Persons.get(id).size();
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
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(outputStreamWriter);
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

    private Collection<SimpleFeature> getAllFeatures(String filename){
        try {
            File dataFile = new File(filename);
            Gbl.assertIf(dataFile.exists());
            ShapefileDataStore store = (ShapefileDataStore) FileDataStoreFinder.getDataStore(dataFile);
            store.setCharset(StandardCharsets.UTF_8);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            SimpleFeatureIterator it = featureSource.getFeatures().features();
            ArrayList featureSet = new ArrayList();

            while(it.hasNext()) {
                SimpleFeature ft = (SimpleFeature)it.next();
                featureSet.add(ft);
            }

            it.close();
            store.dispose();
            return featureSet;
        } catch (IOException var7) {
            throw new UncheckedIOException(var7);
        }
    }
}
