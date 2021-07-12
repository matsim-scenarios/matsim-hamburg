package org.matsim.analysis.mobBug;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TripAnlysis {


    public static void main (String args []) throws IOException {

        //read in BaseCase to and check wich agents where allowed to use the mobilityBudget and how many trips they made
        Population pop = PopulationUtils.readPopulation("D:/Gregor/Uni/TUCloud/Masterarbeit/MATSim/BaseCase/hamburg-v1.1-10pct.output_plans.xml.gz");
        HashMap<Id<Person>, Integer> personsEligibleForMobilityBudgetAndAmountOfTrips = new HashMap<>();

        for (Person person : pop.getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                int numberOfTrips = trips.size();
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                    }
                }
                if (!transportModeList.contains(TransportMode.car)) {
                    personsEligibleForMobilityBudgetAndAmountOfTrips.put(personId, numberOfTrips);
                }
            }
        }

        //reading in policy case and checking if MobBudget was used
        pop = PopulationUtils.readPopulation("D://Gregor//Uni//TUCloud//Masterarbeit//MATSim//Outputs//0.5//hamburg-v1.1-10pct.output_plans.xml.gz");
        HashMap<Id<Person>, Boolean> personUsedMobilityBudget = new HashMap<>();
        for (Person person : pop.getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                    }
                }
                if (!transportModeList.contains(TransportMode.car) && personsEligibleForMobilityBudgetAndAmountOfTrips.containsKey(personId)) {
                    personUsedMobilityBudget.put(personId, true);
                }
                else personUsedMobilityBudget.put(personId, false);
            }
        }

        //writing results
        FileWriter writer = new FileWriter("D://Gregor//Uni//TUCloud//Masterarbeit//Analysen//test.csv");
        writer.write("personId"+","+"amountOfCarTrips"+","+"usedMobBudget");
        writer.append("\n");

        for (Id<Person> i: personsEligibleForMobilityBudgetAndAmountOfTrips.keySet()) {
            writer.append(i.toString()+";"+personsEligibleForMobilityBudgetAndAmountOfTrips.get(i)+";"+personUsedMobilityBudget.get(i));
            writer.append("\n");
        }
        writer.close();

    }

}
