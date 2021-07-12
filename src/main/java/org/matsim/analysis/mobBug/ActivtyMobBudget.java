package org.matsim.analysis.mobBug;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ActivtyMobBudget {

    public static void main(String args[]) throws Exception {
        ArrayList<Id<Person>> list = (ArrayList) readCsv("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\test.csv");
        Population pop = PopulationUtils.readPopulation("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\input\\hamburg-v1.1-1pct.plans.xml.gz");

        for (Id<Person> id: list) {
            Person person = pop.getPersons().get(id);
            System.out.println(person.getId());
            System.out.println(person.getAttributes());
        }
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
