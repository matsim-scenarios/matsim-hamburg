package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

/**
 * @author zmeng whao
 *
 * run NetworkReducer and TransitReducer to in order to decrease the memoery demands
 */
public class ScenarioReducer {

    private final Scenario scenario;

    public ScenarioReducer(String configFile) {
        Config config = ConfigUtils.loadConfig(configFile);
        scenario = ScenarioUtils.loadScenario(config);
    }

    public Scenario getScenario() {
        return scenario;
    }

    public static void main(String[] args) {
        String configFile;
        if(args.length > 0 ){
            configFile = args[0];
        } else
            configFile = "scenarios/input/hamburg-v1.1-10pct.config.xml";

        ScenarioReducer scenarioReducer = new ScenarioReducer(configFile);

        TransitReducerForTest transitReducerForTest = new TransitReducerForTest(Id.create("S1---5111_109", TransitLine.class));

        Scenario scenario1 = transitReducerForTest.reduce(scenarioReducer.getScenario());
        TransitSchedule transitSchedule = scenario1.getTransitSchedule();
        Vehicles transitVehicle = scenario1.getTransitVehicles();

        NetworkReducerForTest networkReducerForTest = new NetworkReducerForTest(556810.114452, 5928984.932974, 623954.716144, 5992509.470133);
        Network network = networkReducerForTest.reduceNetwork(scenarioReducer.getScenario());

        new CreatePseudoNetwork(transitSchedule, network, "pt_").createNetwork();


        NetworkWriter writer = new NetworkWriter(network);
        writer.write("test/input/test-hamburg-with-pt-network.xml.gz");
        TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(transitSchedule);
        transitScheduleWriter.writeFile("test/input/test-hamburg-transitSchedule.xml.gz");
        MatsimVehicleWriter transitVehiclesWriter = new MatsimVehicleWriter(transitVehicle);
        transitVehiclesWriter.writeFile("test/input/test-hamburg-transitVehicles.xml.gz");

    }


}
