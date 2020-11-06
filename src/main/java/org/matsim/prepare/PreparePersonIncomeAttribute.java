package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 * @author zmeng
 */
public class PreparePersonIncomeAttribute {
    private static Logger log = Logger.getLogger(PreparePersonIncomeAttribute.class);

    private Population population;
    private Map<String, Double> personId2income = new HashMap<>();

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.loadConfig("../shared-svn/projects/RealLabHH/matsim-input-files/v1/hamburg-v1.0-25pct.config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        PreparePersonIncomeAttribute preparePersonIncomeAttribute = new PreparePersonIncomeAttribute();
        preparePersonIncomeAttribute.prepareIncome(scenario, "/Users/meng/IdeaProjects/shared-svn/projects/realLabHH/matsim-input-files/v1/person2income.csv");

        PopulationUtils.writePopulation(scenario.getPopulation(), "scenarios/input/population-25pct-withIncome.xml.gz");

    }

    public PreparePersonIncomeAttribute() throws IOException {

    }

    public void prepareIncome(Scenario scenario, String person2incomeFile) throws IOException {
        this.population = scenario.getPopulation();
        this.readPersonId2Income(person2incomeFile);

        final Random rnd = MatsimRandom.getLocalInstance();

        for (Person person :
                this.population.getPersons().values()) {
            if(this.personId2income.containsKey(person.getId().toString())){
                person.getAttributes().putAttribute("income", personId2income.get(person.getId().toString()));
            } else {
                double income = 0.;
                double rndDouble = rnd.nextDouble();
                if (rndDouble <= 0.1) income = 826.;
                else if (rndDouble > 0.1 && rndDouble <= 0.2) income = 1142.;
                else if (rndDouble > 0.2 && rndDouble <= 0.3) income = 1399.;
                else if (rndDouble > 0.3 && rndDouble <= 0.4) income = 1630.;
                else if (rndDouble > 0.4 && rndDouble <= 0.5) income = 1847.;
                else if (rndDouble > 0.5 && rndDouble <= 0.6) income = 2070.;
                else if (rndDouble > 0.6 && rndDouble <= 0.7) income = 2332.;
                else if (rndDouble > 0.7 && rndDouble <= 0.8) income = 2659.;
                else if (rndDouble > 0.8 && rndDouble <= 0.9) income = 3156.;
                else if (rndDouble > 0.9) income = 4329.;
                else {
                    throw new RuntimeException("Aborting..." + rndDouble);
                }
                person.getAttributes().putAttribute("income", income);
            }
        }
    }
    private void readPersonId2Income(String person2incomeFile) throws IOException {
        File file = new File(person2incomeFile);
        log.info("read personId2incomeFile from" + file.getAbsolutePath());

        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String firstLine = csvReader.readLine();
        while ((firstLine = csvReader.readLine()) != null) {
            String[] income = firstLine.split(",");

            this.personId2income.put(income[0],Double.valueOf(income[1]));
        }
        csvReader.close();

    }
}
