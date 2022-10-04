package org.matsim.prepare;

import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;

import java.util.Map;
import org.apache.logging.log4j.LogManager;

/**
 * @author zmeng, haowu
 */
public class NetworkReducerForTest {

    //BoundingBox:
    private final double xMin;
    private final double yMin;
    private final double xMax;
    private final double yMax;

    private static final Logger log = LogManager.getLogger(NetworkReducerForTest.class);

    public NetworkReducerForTest(double xMin, double yMin, double xMax, double yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public Network reduceNetwork(Scenario scenario) {
        Network network = scenario.getNetwork();

        int numOfRemovedLinks = 0;
        log.info("remove links.............: " + numOfRemovedLinks);
        for (Map.Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
            Link link = entry.getValue();
            if((link.getAllowedModes().contains(TransportMode.pt)) || outOfBound(link)){
                network.removeLink(entry.getKey());
                numOfRemovedLinks++;
            }
        }
        log.info("remove links.............: " + numOfRemovedLinks + "....end");


        (new NetworkCleaner()).run(network);

        return network;
    }

    public static void main(String[] args) {

    }

    private boolean outOfBound(Link link) {
        double x = link.getCoord().getX();
        double y = link.getCoord().getY();

        if(x < xMin || x > xMax || y < yMin || y > yMax){
            return true;
        } else {
            return false;
        }
    }


}
