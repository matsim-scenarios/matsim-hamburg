/*
package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.Boundary;
import org.opengis.geometry.BoundingBox;

import java.util.*;


public class SelectionMobilityBudget {

    private static final Logger log = Logger.getLogger(SelectionMobilityBudget.class);


    public static void filterForRegion(Population population, String shapeFile, Map<Id<Person>, Double> personsEligibleForMobilityBudget) {


        //List<Geometry> gg = ShpGeometryUtils.loadGeometries(IOUtils.resolveFileOrResource(""));
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        //BoundingBox box = null;
        MultiPolygon geometry =null;

        BoundingBox box = null;
        Boundary boundary = null;
        //BoundingBox box;
        for (SimpleFeature feature: features) {
            if (feature.getAttribute("name").equals( "Hamburg_city")) {
                geometry = (MultiPolygon) feature.getDefaultGeometry();
                //box = feature.getBounds();
            }
        }

        List<Id<Person>> selectedAgents = new ArrayList();
        for (Person p: population.getPersons().values()) {
            if (personsEligibleForMobilityBudget.containsKey(p.getId())) {

                TripStructureUtils.StageActivityHandling stageActivityHandling = TripStructureUtils.StageActivityHandling.ExcludeStageActivities;
                List<Activity> activities = PopulationUtils.getActivities(p.getSelectedPlan(), stageActivityHandling);
                for (Activity activty: activities) {
                    GeometryFactory geometryFactory = new GeometryFactory();
                    //if (activty.getType().contains("home") && box.contains(activty.getCoord().getX(), activty.getCoord().getY()) ) {
                    if (activty.getType().contains("home") && geometry.contains(geometryFactory.createPoint(new Coordinate(activty.getCoord().getX(),activty.getCoord().getY())))) {
                        log.info("Agent: " + p.getId()+"with activity"+ activty.getType() +" is in ShapeFile");
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
            log.info("Removed: " + personId + " because he is not in the Shape File");
        }

    }


    public static void  incomeBasedSelection(Population population, double percentage, Map<Id<Person>, Double> personsEligibleForMobilityBudget) {

        LinkedHashMap<Id<Person>, Double> incomeBasedSelectedAgents = new LinkedHashMap<>();

        for (Person p: population.getPersons().values()) {

            if (personsEligibleForMobilityBudget.containsKey(p.getId())) {
                incomeBasedSelectedAgents.put(p.getId(), (Double) p.getAttributes().getAttribute("income"));
            }
        }

        int sizeOfMap = incomeBasedSelectedAgents.size();
        int numberOfAgents = (int) Math.ceil(sizeOfMap * percentage);
        Map<Id<Person>, Double> sortedIncomeBasedSelection = sortByValue(incomeBasedSelectedAgents);



        int counter = 0;
        List<Id<Person>> agentsToKeep = new ArrayList();
        for (Id<Person> personId: sortedIncomeBasedSelection.keySet()) {
            if (counter < numberOfAgents) {
                agentsToKeep.add(personId);
            }
            else break;
            counter++;
        }

        List<Id<Person>> toRemove = new ArrayList();
        for( Id<Person> p: personsEligibleForMobilityBudget.keySet() ) {
            if (!agentsToKeep.contains(p)) {
                toRemove.add(p);
            }
        }

        for (Id<Person> personId: toRemove) {
            personsEligibleForMobilityBudget.remove(personId);
            log.info("Removed: " + personId + "because of his income");
        }

    }


    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
*/
