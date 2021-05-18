package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author zmeng
 */
public class GenerateStopId2StopName {
    public static void main(String[] args) throws IOException {
        String outputFile = "scenarios/stopId2StopName.csv";
        String splitSymbol =";";
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(outputStreamWriter);
        bw.write("id" + splitSymbol + "name" );

        Config config = ConfigUtils.loadConfig("../shared-svn/projects/RealLabHH/matsim-input-files/v1/hamburg-v1.1-1pct.config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        for (TransitStopFacility tr :
                scenario.getTransitSchedule().getFacilities().values()) {
            bw.newLine();
            bw.write(tr.getId() + splitSymbol + tr.getName());
        }

        bw.close();

    }
}
