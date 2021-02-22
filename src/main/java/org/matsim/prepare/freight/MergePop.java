package org.matsim.prepare.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.prepare.freight.CreatFreightAgents.COMMERCIAL;
import static org.matsim.run.RunBaseCaseHamburgScenario.VERSION;

/**
 * @author zmeng
 */
public class MergePop {
    public static void main(String[] args) {

        if(args.length == 0){
            args = new String[]{"/Users/meng/work/realLabHH_meng/files/v3/h-25pct-36.output_plans.xml.gz",
                    "/Users/meng/work/realLabHH_meng/files/v3/hamburg-commercial-25pct.plans.xml.gz",
                    "/Users/meng/work/realLabHH_meng/files/v3/"
            };
        }
        // subpopulation: persons
        final String inputFile1 = args[0];

        // subpopulation: freight
        final String inputFile2 = args[1];

        final String populationOutputFileName = args[2];

        Config config1 = ConfigUtils.createConfig();
        config1.plans().setInputFile(inputFile1);
        config1.global().setCoordinateSystem("EPSG:25832");
        config1.plans().setInputCRS("EPSG:25832");
        Scenario scenario1 = ScenarioUtils.loadScenario(config1);

        Config config2 = ConfigUtils.createConfig();
        config2.plans().setInputFile(inputFile2);
        config2.global().setCoordinateSystem("EPSG:25832");
        config2.plans().setInputCRS("EPSG:25832");
        Scenario scenario2 = ScenarioUtils.loadScenario(config2);

        Scenario scenario3 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario3.getConfig().plans().setInputCRS("EPSG:25832");
        scenario3.getConfig().global().setCoordinateSystem("EPSG:25832");
        Population population = scenario3.getPopulation();

        for (Person person: scenario1.getPopulation().getPersons().values()) {
            population.addPerson(person);
            population.getPersons().get(person.getId()).getAttributes().putAttribute(scenario3.getConfig().plans().getSubpopulationAttributeName(), "person");
        }
        for (Person person : scenario2.getPopulation().getPersons().values()) {
            population.addPerson(person);
            population.getPersons().get(person.getId()).getAttributes().putAttribute(scenario3.getConfig().plans().getSubpopulationAttributeName(), COMMERCIAL);
        }

        PopulationWriter writer = new PopulationWriter(population);
        writer.write(populationOutputFileName + "hamburg-" + VERSION +"-25pct.plans.xml.gz");

        // downscale to 10%
        PopulationUtils.sampleDown(population, 0.4);
        PopulationUtils.writePopulation(population,populationOutputFileName + "hamburg-" + VERSION +"-10pct.plans.xml.gz");

        // downscale to 1%
        PopulationUtils.sampleDown(population, 0.1);
        PopulationUtils.writePopulation(population,populationOutputFileName + "hamburg-" + VERSION +"-1pct.plans.xml.gz");

    }
}
