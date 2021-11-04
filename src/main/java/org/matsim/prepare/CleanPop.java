package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author zmeng
 */
public class CleanPop {
    public static void main(String[] args) {
        String folder;
        String planFile;
        String version;
        if(args.length == 0){
            folder = "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.2/input/";
            planFile = "hamburg-v2.2-10pct.input-plans.xml.gz";
            version = "v2.2";
        } else {
            folder = args[0];
            planFile = args[1];
            version = args[2];
        }

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(folder + planFile);

        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputCRS("EPSG:25832");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        for (Person person :
                population.getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            person.getPlans().clear();
            person.addPlan(selectedPlan);
            person.setSelectedPlan(selectedPlan);
        }

        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), folder + "hamburg-" + version + "-10pct.input-plans.xml.gz");

//        // sample 25% to 10%
//        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.4);
//        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(),folder + "hamburg-" + version + "-10pct.plans.xml.gz");
//
//        // sample 10% to 1%
//        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.1);
//        PopulationUtils.writePopulation(scenario.getPopulation(), folder + "hamburg-" + version + "-1pct.plans.xml.gz");
    }
}
