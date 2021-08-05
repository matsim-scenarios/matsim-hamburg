package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SelectionMobilityBudget {


    public static void main (String[] args) {

        String shapeFile = "C:\\Users\\Gregor\\Documents\\shared-svn\\projects\\realLabHH\\data\\hamburg_shapeFile\\hamburg_metropo\\hamburg_metropo.shp";
        Population population = PopulationUtils.readPopulation("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\input\\hamburg-v1.0-1pct.plans.xml.gz");

        //HashMap<Id, Double> idDoubleHashMap = filterForRegion(population, shapeFile);
        HashMap<Id, Double> idDoubleHashMap = incomeBasedSelection(population, 10.0);
    }

    public static HashMap<Id, Double> filterForRegion(Population population, String shapeFile, Map<Id<Person>, Double> personsEligibleForMobilityBudget) {

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        BoundingBox box = null;
        for (SimpleFeature feature: features) {
            if (feature.getAttribute("name").equals( "Hamburg_city")) {
                box = feature.getBounds();
            }
        }


        HashMap<Id, Double> selectedAgents = new HashMap<>();

        for (Person p: population.getPersons().values()) {

            TripStructureUtils.StageActivityHandling stageActivityHandling = TripStructureUtils.StageActivityHandling.ExcludeStageActivities;
            List<Activity> activities = PopulationUtils.getActivities(p.getSelectedPlan(), stageActivityHandling);
            for (Activity activty: activities) {
                if (activty.getType().contains("home") && box.contains(activty.getCoord().getX(), activty.getCoord().getY()) ) {
                    selectedAgents.put(p.getId(),0.0);
                }
            }

        }

        return selectedAgents;
    }


    static HashMap<Id, Double> incomeBasedSelection(Population population, double percentage) {

        HashMap<Id, Double> incomeBasedSelectedAgents = new HashMap<Id, Double>();

        for (Person p: population.getPersons().values()) {
            incomeBasedSelectedAgents.put(p.getId(), (Double) p.getAttributes().getAttribute("income"));
        }
        
        incomeBasedSelectedAgents.entrySet().stream()
                .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
                .forEach(k -> System.out.println(k.getKey() + ": " + k.getValue()));

        return incomeBasedSelectedAgents;
    }
}
