package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zmeng
 */
public class TransitReducerForTest {

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();

        config.network().setInputFile("");
        config.transit().setTransitScheduleFile("");
        config.transit().setVehiclesFile("");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        List<TransitLine> reTrs = new LinkedList<TransitLine>();
        for(TransitLine tr: scenario.getTransitSchedule().getTransitLines().values()){
            if(!tr.getName().equals("u8")){
                reTrs.add(tr);
            }
        }

        for (TransitLine tr : reTrs) {
            scenario.getTransitSchedule().removeTransitLine(tr);
        }

        TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(scenario.getTransitSchedule());
        transitScheduleWriter.writeFile("");
    }
}
