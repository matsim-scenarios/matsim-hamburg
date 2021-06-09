package org.matsim.analysis;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import java.io.FileReader;
import java.util.*;

public class RailwayCrossings {

    private static String inputFile ="D:/Arbeit/shared-svn/projects/realLabHH/data/Bahnübergänge/loaded_lcs.csv";
    private static String networkFile ="https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-network-with-pt.xml.gz";
    private static String eventFile = "";
    //static HashMap<Tuple, Tuple> pair = new HashMap();
    static HashMap<Id, Double> carLinkMap = new HashMap<>();
    static HashMap<Id, Double> carLinkTimeMap = new HashMap<>();
    static HashMap<Id, Double> ptLinkMap = new HashMap<>();
    static HashMap<Id, Double> ptLinkTimeMap = new HashMap<>();
    static HashMap<Id, Id> conection = new HashMap<>();



    public static void main(String args[]) throws Exception {

        //read input csv file
        List<Coord> coordinates = readCsv(inputFile);
        //read in networks and filter for modes car and pt
        Network network = NetworkUtils.readNetwork(networkFile);
        TransportModeNetworkFilter transportModeNetworkFilter = new TransportModeNetworkFilter(network);
        Network ptNetwork = NetworkUtils.createNetwork();
        transportModeNetworkFilter.filter(ptNetwork, new HashSet(Arrays.asList(TransportMode.pt)));

        Network carNetwork = NetworkUtils.createNetwork();
        transportModeNetworkFilter.filter(carNetwork, new HashSet(Arrays.asList(TransportMode.car)));




        for (int i = 0; i < coordinates.size(); i++) {
            Link l = NetworkUtils.getNearestLink(carNetwork, coordinates.get(i));
            Link nearestPtLink = NetworkUtils.getNearestLink(ptNetwork, coordinates.get(i));
            calculateIntersection(l, nearestPtLink);
        }

        EventsManager events = EventsUtils.createEventsManager();
        RailwayCrossingsEventHandler railwayCrossingsEventHandler = new RailwayCrossingsEventHandler(carLinkMap,carLinkTimeMap,ptLinkMap,ptLinkTimeMap, conection );
        events.addHandler(railwayCrossingsEventHandler);
        new MatsimEventsReader(events).readFile(eventFile);


        }





    private static Coord calculateIntersection(Link carLink, Link ptLink) {

        Coord coordOfIntersection = null;

        //transforming Coord into linear equations
        double a1 = carLink.getToNode().getCoord().getY() - carLink.getFromNode().getCoord().getY();
        double b1 = carLink.getFromNode().getCoord().getX() - carLink.getToNode().getCoord().getX();
        double c1 = a1 *carLink.getFromNode().getCoord().getX() + b1 *carLink.getFromNode().getCoord().getY();

        double a2 = ptLink.getToNode().getCoord().getY() - ptLink.getFromNode().getCoord().getY();
        double b2 = ptLink.getFromNode().getCoord().getX() - ptLink.getToNode().getCoord().getX();
        double c2 = a2 *ptLink.getFromNode().getCoord().getX() + b2 *ptLink.getFromNode().getCoord().getY();

        double det = a1*b2- a2*b1;

        if (det == 0) {
            //Lines are parallel
        } else {
            double x = (b2 * c1 - b1 * c2) / det;
            double y = (a1 * c2 - a2 * c1) / det;
            //check if intersection is on the link
            if (carLink.getFromNode().getCoord().getX() < x && x < carLink.getToNode().getCoord().getX() && carLink.getFromNode().getCoord().getY() < y && y < carLink.getToNode().getCoord().getY() ) {
                coordOfIntersection = new Coord(x,y);
                //Tuple<Link, Link> linkTuple = new Tuple(carLink , ptLink );
                double distance1 = NetworkUtils.getEuclideanDistance(carLink.getFromNode().getCoord(), coordOfIntersection);
                carLinkMap.put(carLink.getId(), distance1);
                double distance2 = NetworkUtils.getEuclideanDistance(ptLink.getFromNode().getCoord(), coordOfIntersection);
                ptLinkMap.put(ptLink.getId(), distance2);
                //Tuple<Link, Link> distanceTuple = new Tuple(distance1 , distance2 );

            }

            System.out.println("Lines dont cross on link");

        }
        return coordOfIntersection;
    }



    private static List<Coord> readCsv(String fileName) throws Exception {
        List<Coord> coordsOfCrossings = new ArrayList<Coord>();
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        // if your csv file doesn't have header line, remove withSkipLines(1)
        try (CSVReader reader = new CSVReaderBuilder(
                new FileReader(fileName)).withCSVParser(csvParser).withSkipLines(1).build()) {
                List<String[]> r = reader.readAll();
                for (int i = 0; i<r.size(); i++) {
                    String[] test = r.get(i);
                    Coord coord = new Coord(Double.parseDouble(test[3]), Double.parseDouble(test[4]));
                    coordsOfCrossings.add(coord);
                }
            return coordsOfCrossings;
        }
    }


}
