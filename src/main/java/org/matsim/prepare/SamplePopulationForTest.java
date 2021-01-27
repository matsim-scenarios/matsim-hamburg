package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.run.RunBaseCaseHamburgScenario.VERSION;

public class SamplePopulationForTest {

    public static void main(String args []) {
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile("D:/Gregor/Uni/TUCloud/Masterarbeit/MATSim/input/hamburg-v1.0-1pct.plans.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population pop =  scenario.getPopulation();
        PopulationUtils.sampleDown(pop, 0.5);
        String output = "D:/Gregor/Uni/TUCloud/Masterarbeit/MATSim/input/testPop.xml";
        PopulationUtils.writePopulation(pop, output);
    }
}
