package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

import java.util.*;


public class SelectionMobilityBudget {

    private static final Logger log = Logger.getLogger(SelectionMobilityBudget.class);


    public static void main (String[] args) {

        //String shapeFile = "C:\\Users\\Gregor\\Documents\\shared-svn\\projects\\realLabHH\\data\\hamburg_shapeFile\\hamburg_metropo\\hamburg_metropo.shp";
        //Population population = PopulationUtils.readPopulation("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\input\\hamburg-v1.0-1pct.plans.xml.gz");

        //HashMap<Id, Double> idDoubleHashMap = filterForRegion(population, shapeFile);
        //HashMap<Id, Double> idDoubleHashMap = incomeBasedSelection(population, 10.0);
    }

    public static void filterForRegion(Population population, String shapeFile, Map<Id<Person>, Double> personsEligibleForMobilityBudget) {

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        BoundingBox box = null;
        for (SimpleFeature feature: features) {
            if (feature.getAttribute("name").equals( "Hamburg_city")) {
                box = feature.getBounds();
            }
        }

        List<Id<Person>> selectedAgents = new ArrayList();
        for (Person p: population.getPersons().values()) {
            if (personsEligibleForMobilityBudget.containsKey(p.getId())) {
                TripStructureUtils.StageActivityHandling stageActivityHandling = TripStructureUtils.StageActivityHandling.ExcludeStageActivities;
                List<Activity> activities = PopulationUtils.getActivities(p.getSelectedPlan(), stageActivityHandling);
                for (Activity activty: activities) {
                    if (activty.getType().contains("home") && box.contains(activty.getCoord().getX(), activty.getCoord().getY()) ) {
                        log.info("Agent: " + p.getId()+" is in ShapeFile");
                        selectedAgents.add(p.getId());
                    }
                }
            }
        }

        List<Id<Person>> toRemove = new ArrayList();
        for( Id<Person> p: personsEligibleForMobilityBudget.keySet() ) {
            if (!selectedAgents.contains(p)) {
                toRemove.add(p);
            }
        }

        for (Id<Person> personId: toRemove) {
            personsEligibleForMobilityBudget.remove(personId);
            log.info("Removed: " + personId);
        }

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
