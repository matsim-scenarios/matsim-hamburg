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
import java.util.List;

public class ElegibleAgentsForMobBudToCSV {

    public static void main(String args []) throws IOException {

        //read in BaseCase to and check wich agents where allowed to use the mobilityBudget and how many trips they made
        Population pop = PopulationUtils.readPopulation("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\input\\hamburg-v1.1-1pct.plans.xml.gz");
        ArrayList<Id<Person>> personsEligibleForMobilityBudget = new ArrayList<>();

        for (Person person : pop.getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        System.out.println(leg.getMode());
                        transportModeList.add(leg.getMode());
                    }
                }
                if (transportModeList.contains(TransportMode.car)) {
                    System.out.println("True");
                    personsEligibleForMobilityBudget.add(personId);
                }
            }
        }
        writeList2CSV(personsEligibleForMobilityBudget);

    }

    private static void writeList2CSV (List originalList) throws IOException {
        FileWriter writer = new FileWriter("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\test.csv");
        writer.write("personId");
        writer.append("\n");

        for (int i = 0; i < originalList.size(); i++) {
            writer.append((String) originalList.get(i).toString());
            writer.append("\n");
        }
        writer.close();
    }

}
