package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;

import java.io.IOException;
import java.text.ParseException;

/**
 * @author zmeng
 */
public class RunRealLabHH2030Scenario {

    private static final Logger log = Logger.getLogger(RunRealLabHH2030Scenario.class);

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"scenarios/input/hamburg-v1.1-10pct.config.xml"};
        }

        RunRealLabHH2030Scenario realLabHH2030 = new RunRealLabHH2030Scenario();
        realLabHH2030.run(args);
    }

    private void run(String[] args) throws IOException {

        Config config = prepareConfig(args);
        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();
        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        Controler controler =  RunBaseCaseHamburgScenario.prepareControler(scenario);
        return controler;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);
        return config;
    }

    public static Scenario prepareScenario(Config config) throws IOException {
        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        return scenario;
    }
}
