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
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

import java.util.*;

/**
 * @author zmeng, haowu
 */
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
            networkOutputFile = "test/input/test/test-hamburg-v1.1-network-with-pt.xml.gz";
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

        // Get pt subnetwork
        log.info("read pt network from " + networkInputFile);
        Scenario ptScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransportModeNetworkFilter transportModeNetworkFilterPt = new TransportModeNetworkFilter(scenario.getNetwork());
        transportModeNetworkFilterPt.filter(ptScenario.getNetwork(), new HashSet<>(Arrays.asList(TransportMode.pt)));

        //for(Link link : scenario.getNetwork().getLinks().values()){
        int numOfRemovedLinks = 0;
        log.info("remove links.............: " + numOfRemovedLinks);
        for (Map.Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
            Link link = entry.getValue();
            if((!link.getAllowedModes().contains(TransportMode.pt)) && outOfBound(link)){
                scenario.getNetwork().removeLink(entry.getKey());
                numOfRemovedLinks++;
            }
        }
        log.info("remove links.............: " + numOfRemovedLinks + "....end");

        //todo @Hao: please find a way to clean the network with pt lines

        // Get car subnetwork and clean it
        Scenario carScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransportModeNetworkFilter transportModeNetworkFilterCar = new TransportModeNetworkFilter(scenario.getNetwork());
        transportModeNetworkFilterCar.filter(carScenario.getNetwork(), new HashSet<>(Arrays.asList(TransportMode.car)));
        (new NetworkCleaner()).run(carScenario.getNetwork());
        log.info("Finished creating and cleaning car subnetwork");

        // Add pt back into the other network
        // *** Note: Customized attributes are not considered here ***
        NetworkFactory factory = carScenario.getNetwork().getFactory();
        for (Node node : ptScenario.getNetwork().getNodes().values()) {
            Node node2 = factory.createNode(node.getId(), node.getCoord());
            carScenario.getNetwork().addNode(node2);
        }
        for (Link link : ptScenario.getNetwork().getLinks().values()) {
            Node fromNode = carScenario.getNetwork().getNodes().get(link.getFromNode().getId());
            Node toNode = carScenario.getNetwork().getNodes().get(link.getToNode().getId());
            Link link2 = factory.createLink(link.getId(), fromNode, toNode);
            link2.setAllowedModes(link.getAllowedModes());
            link2.setCapacity(link.getCapacity());
            link2.setFreespeed(link.getFreespeed());
            link2.setLength(link.getLength());
            link2.setNumberOfLanes(link.getNumberOfLanes());
            carScenario.getNetwork().addLink(link2);
        }
        log.info("Finished merging pt network layer back into network");

        // Write modified network to file
        NetworkWriter writer = new NetworkWriter(carScenario.getNetwork());

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
