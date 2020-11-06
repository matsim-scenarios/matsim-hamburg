package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author zmeng
 */
public class GeneratePersonAttributes {

    private static final Logger log =Logger.getLogger(GeneratePersonAttributes.class);
    Map<String, Map<Id<Person>, String>> attribute2person2value = new HashMap<>();

    private Scenario scenario;

    public GeneratePersonAttributes(String plansFile, List<String> attributesName) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputCRS(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputFile(plansFile);
        this.scenario = ScenarioUtils.loadScenario(config);
        for (String str :
                attributesName) {
            attribute2person2value.put(str, new HashMap<>());
        }
    }

    public static void main(String[] args) throws IOException {

        String inputPlan = "../shared-svn/projects/realLabHH/matsim-input-files/v1/hamburg-v1.0-25pct.plans.xml.gz";
        String outputResult = "scenarios/initialDemandCalibration/";
        GeneratePersonAttributes generatePersonAttributes = new GeneratePersonAttributes(inputPlan, List.of("age","gender"));
        generatePersonAttributes.generate();
        generatePersonAttributes.write(outputResult,",");

    }

    private void generate() {
        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            for (String str :
                    this.attribute2person2value.keySet()) {
                this.attribute2person2value.get(str).put(person.getId(), person.getAttributes().getAttribute(str).toString());
            }
        }
    }

    private void write(String outputPath, String splitSymbol) throws IOException {
        for (String str :
                this.attribute2person2value.keySet()) {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputPath + str + ".csv"), StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(outputStreamWriter);
            bw.write("person" + splitSymbol + "value" );

            this.attribute2person2value.get(str).forEach((personId, string) -> {
                try {
                    bw.newLine();
                    bw.write(personId + splitSymbol + string);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bw.close();
        }
    }
}
