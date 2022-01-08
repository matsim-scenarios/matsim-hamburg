package org.matsim.analysis.preAnalysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(
        name = "analyze-population",
        description = "Extract the home location of the persons in the population file"
)
public class PopulationAnalysis implements MATSimAppCommand {
    @CommandLine.Option(names = "--population", description = "Path to input population", required = true)
    private String populationPath;

    @CommandLine.Option(names = "--output", description = "Path to output folder", required = true)
    private Path outputPath;

    public static void main(String[] args) {
        new PopulationAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Population population = PopulationUtils.readPopulation(populationPath.toString());
        summarizePersonAttribute(population);
        return 0;
    }


    private void summarizePersonAttribute(Population population) throws IOException {
        List<Person> personsToAnalyze = new ArrayList<>(population.getPersons().values());
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputPath.toString()), CSVFormat.TDF);
        csvWriter.printRecord("person", "age", "sex", "household_size", "household_income_group", "estimated_personal_allowance", "number_of_trips_per_day", "home_location");

        for (Person person : personsToAnalyze) {
            Double income = PersonUtils.getIncome(person); // This value may be null;
            Integer age = PersonUtils.getAge(person); // This value may be null;
            String sex = PersonUtils.getSex(person);
            String incomeGroup = (String) person.getAttributes().getAttribute("householdincome");
            String householdSize = (String) person.getAttributes().getAttribute("householdsize");
            String homeLocation;
            if (person.getAttributes().getAttribute("homeLocation").toString().equals("inside")) {
                homeLocation = "inside";
            } else if (person.getAttributes().getAttribute("homeLocation").toString().equals("outside")) {
                homeLocation = "outside";
            } else {
                homeLocation = "unknown";
            }

            if (income == null) {
                income = -1.0;
            }

            if (age == null) {
                age = -1;
            }

            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            int numOfTripsPerDay = trips.size();

            csvWriter.printRecord(person.getId().toString(), age.toString(), sex,
                    householdSize, incomeGroup, income.toString(), Integer.toString(numOfTripsPerDay), homeLocation);
        }
    }
}
