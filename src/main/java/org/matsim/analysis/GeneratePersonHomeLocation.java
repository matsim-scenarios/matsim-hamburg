package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBaseCaseHamburgScenario;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zmeng
 */
public class GeneratePersonHomeLocation {

    private static final Logger log =Logger.getLogger(GeneratePersonHomeLocation.class);
    private Map<Person, Coord> person2homeLocation = new HashMap<>();

    private Scenario scenario;


    public static void main(String[] args) throws IOException {

        String inputPlan = "../shared-svn/projects/realLabHH/matsim-input-files/v1/hamburg-v1.0-25pct.plans.xml.gz";
        String outputResult = "../shared-svn/projects/realLabHH/matsim-input-files/v1/person2homeLocation.csv";
        GeneratePersonHomeLocation generatePersonHomeLocation = new GeneratePersonHomeLocation(inputPlan);
        generatePersonHomeLocation.generate();
        generatePersonHomeLocation.write(outputResult,",");

    }

    public GeneratePersonHomeLocation(String plansFile) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputCRS(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputFile(plansFile);
        this.scenario = ScenarioUtils.loadScenario(config);
    }

    private void generate() {
        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            var activities = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
            for (Activity act :
                    activities) {
                if (act.getType().contains("home")){
                    this.person2homeLocation.put(person, act.getCoord());
                    break;
                }
            }
        }
    }

    private void write(String outputFile, String splitSymbol) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(outputStreamWriter);
        bw.write("person" + splitSymbol + "home_x" + splitSymbol + "home_y");

        this.person2homeLocation.forEach((person, coord) -> {
            try {
                bw.newLine();
                bw.write(person.getId().toString() + splitSymbol + coord.getX() + splitSymbol + coord.getY());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        bw.close();
    }
}
