package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Map;

public class NetworkReducerForTest {

    //BoundingBox:
    private static final double xMin = 556810.114452;
    private static final double yMin = 5928984.932974;
    private static final double xMax = 623954.716144;
    private static final double yMax = 5992509.470133;

    private static final Logger log = Logger.getLogger(NetworkReducerForTest.class);

    public static void main(String[] args) {

        String networkInputFile;
        String networkOutputFile;

        if(args.length == 0){
            networkInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-network-with-pt.xml.gz";
            networkOutputFile = "test/input/test-hamburg-with-pt.xml.gz";
        } else {
            networkInputFile = args[0];
            networkOutputFile = args[1];
        }


//        Geometry areaGeometry = new GeometryFactory().createPolygon(new Coordinate[]{
//                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
//                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
//                new Coordinate(xMin, yMin)
//        });

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());

        log.info("read network from " + networkInputFile);
        reader.readFile(networkInputFile);

        Network network = scenario.getNetwork();

        //for(Link link : scenario.getNetwork().getLinks().values()){
        int numOfRemovedLinks = 0;
        log.info("remove links.............: " + numOfRemovedLinks);
        for (Map.Entry<Id<Link>, ? extends Link> entry : network.getLinks().entrySet()) {
            Link link = entry.getValue();
            if((!link.getAllowedModes().contains(TransportMode.pt)) && outOfBound(link)){
                scenario.getNetwork().removeLink(entry.getKey());
                numOfRemovedLinks++;
            }
        }
        log.info("remove links.............: " + numOfRemovedLinks + "....end");

        //todo @Hao: please find a way to clean the network with pt lines
        new NetworkCleaner().run(scenario.getNetwork());
        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());

        log.info("write new network to " + networkOutputFile);
        writer.write(networkOutputFile);
        log.info("finish");
    }

    private static boolean outOfBound(Link link) {
        double x = link.getCoord().getX();
        double y = link.getCoord().getY();

        if(x < xMin || x > xMax || y < yMin || y > yMax){
            return true;
        } else {
            return false;
        }
    }

}
