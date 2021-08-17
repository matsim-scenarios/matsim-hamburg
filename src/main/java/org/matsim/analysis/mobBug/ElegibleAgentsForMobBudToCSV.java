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

        String popFile = args[0];
        //read in BaseCase to and check wich agents where allowed to use the mobilityBudget and how many trips they made
       // Population pop = PopulationUtils.readPopulation("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\input\\h-v2-10pct-accEcc-c4.output_plans.xml.gz");
        Population pop = PopulationUtils.readPopulation(popFile);
        ArrayList<Id<Person>> personsEligibleForMobilityBudget = new ArrayList<>();

        int agentCounter = 0;

        for (Person person : pop.getPersons().values()) {
            agentCounter++;
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                int carCounter = 0;
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                        if (leg.getMode().equals(TransportMode.car)) {
                            carCounter++;
                        }
                    }
                }
                if (transportModeList.contains(TransportMode.car)) {
                    personsEligibleForMobilityBudget.add(personId);
                }
            }
        }
        System.out.println(agentCounter);
        writeList2CSV(personsEligibleForMobilityBudget);
    }

    private static void writeList2CSV (List originalList) throws IOException {
        FileWriter writer = new FileWriter("personIdElegibleForMobBud.csv");
        writer.write("personId");
        writer.append("\n");

        for (int i = 0; i < originalList.size(); i++) {
            writer.append((String) originalList.get(i).toString());
            writer.append("\n");
        }
        writer.close();
    }

}
