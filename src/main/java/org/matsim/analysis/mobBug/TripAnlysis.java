package org.matsim.analysis.mobBug;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TripAnlysis {

    static String popFile;
    static String csvFile;
    static String outputFile;
    private static final Logger log = Logger.getLogger(TripAnlysis.class);


    public static void main (String[] args) throws Exception {

        popFile = args[0];
        csvFile =args[1];
        outputFile =args[2];

        log.info(popFile);
        log.info(csvFile);

        log.info(outputFile);

        //read in BaseCase to and check wich agents where allowed to use the mobilityBudget and how many trips they made
        Population pop = PopulationUtils.readPopulation(popFile);
        ArrayList<Id<Person>> list = (ArrayList) readCsv(csvFile);

        HashMap<Id<Person>, Integer> personsEligibleForMobilityBudgetAndAmountOfTrips = new HashMap<>();
        HashMap<Id, Integer> personsMobilityBudgetAndAge = new HashMap<>();
        HashMap<Id, Double> personsMobilityBudgetAndIncome = new HashMap<>();
        HashMap<Id, String> personsMobilityBudgetAndGender = new HashMap<>();
        HashMap<Id, String> personsMobilityBudgetAndActivity = new HashMap<>();
        HashMap<Id, Coord> personsMobilityBudgetAndActivityCoord = new HashMap<>();


        for (Person person : pop.getPersons().values()) {
            Id personId = person.getId();
            if(list.contains(personId)) {
                Plan plan = person.getSelectedPlan();
                Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
                List<Leg> listLegs = TripStructureUtils.getLegs(plan);
                int numberOfCarLegs = 0;
                for (Leg leg: listLegs) {
                    if (leg.getMode().equals(TransportMode.car)) {
                        numberOfCarLegs++;
                    }
                }

                personsEligibleForMobilityBudgetAndAmountOfTrips.put(personId, numberOfCarLegs);
                personsMobilityBudgetAndIncome.put(personId, (Double) person.getAttributes().getAttribute("income"));
                personsMobilityBudgetAndAge.put(personId, (Integer) person.getAttributes().getAttribute("age"));
                personsMobilityBudgetAndGender.put(personId, (String) person.getAttributes().getAttribute("gender"));
                personsMobilityBudgetAndActivity.put(personId, activity.getType());
                personsMobilityBudgetAndActivityCoord.put(personId, activity.getCoord());

            }
        }
        //writing results
        FileWriter writer = new FileWriter(outputFile);
        writer.write("personId"+","+"amountOfCarTrips"+","+"income"+","+"gender"+","+"age"+","+"activity"+","+"xCoord"+","+"yCoord");
        writer.append("\n");

        for (Id<Person> i: personsEligibleForMobilityBudgetAndAmountOfTrips.keySet()) {
            writer.append(i.toString()+","+personsEligibleForMobilityBudgetAndAmountOfTrips.get(i)+","+personsMobilityBudgetAndIncome.get(i)
            +","+personsMobilityBudgetAndGender.get(i)+","+personsMobilityBudgetAndAge.get(i)+","+personsMobilityBudgetAndActivity.get(i)+
                    ","+personsMobilityBudgetAndActivityCoord.get(i).getX()+","+personsMobilityBudgetAndActivityCoord.get(i).getY());
            writer.append("\n");
        }
        writer.close();

    }

    private static List<Id<Person>> readCsv(String fileName) throws Exception {
        List<Id<Person>> listOfPersonIDs = new ArrayList<>();
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        // if your csv file doesn't have header line, remove withSkipLines(1)
        try (CSVReader reader = new CSVReaderBuilder(
                new FileReader(fileName)).withCSVParser(csvParser).withSkipLines(1).build()) {
            List<String[]> r = reader.readAll();
            for (String[] test : r) {
                listOfPersonIDs.add(Id.createPersonId(test[0]));
            }
            return listOfPersonIDs;
        }
    }

}
