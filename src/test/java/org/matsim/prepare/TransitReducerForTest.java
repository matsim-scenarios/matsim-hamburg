package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zmeng, haowu
 */
public class TransitReducerForTest {

    private static final Logger log = Logger.getLogger(TransitReducerForTest.class);

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();

        config.network().setInputFile("test/input/Test/test-hamburg-v1.1-network-with-pt.xml.gz");
        config.transit().setTransitScheduleFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1-transitSchedule.xml.gz");
        config.transit().setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1-transitVehicles.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);


/*        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        log.info("read network from " + config.network().getInputFile());
        networkReader.readFile(config.network().getInputFile());*/


        List<TransitLine> reTrs = new LinkedList<>();
        for(TransitLine tr: scenario.getTransitSchedule().getTransitLines().values()){
            if(!tr.getId().equals(Id.create("S1---5111_109",TransitLine.class))){
                reTrs.add(tr);
            }
            //System.out.println( "1" );
        }

        for (TransitLine tr : reTrs) {
            scenario.getTransitSchedule().removeTransitLine(tr);
        }

        TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(scenario.getTransitSchedule());
        transitScheduleWriter.writeFile("test/input/Test/WithLessPTLines/hamburg-v1-transitSchedule.xml.gz");
    }
}
