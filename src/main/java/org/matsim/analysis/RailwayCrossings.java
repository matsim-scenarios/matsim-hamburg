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
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.FileReader;
import java.util.*;

public class RailwayCrossings {

    private static String eventFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-10pct/output/hamburg-v1.1-10pct.output_events.xml.gz";
    //static HashMap<Tuple, Tuple> pair = new HashMap();
    static HashMap<Id, Double> carLinkMap = new HashMap<>();
    static HashMap<Id, List<Double>> carLinkTimeMap = new HashMap<>();
    static HashMap<Id, Double> ptLinkMap = new HashMap<>();
    static HashMap<Id, List<Double>> ptLinkTimeMap = new HashMap<>();
    static HashMap<Id, Id> connection = new HashMap<>();
    static double offSet = 10.0;



    public static void main(String[] args) throws Exception {

        //read input csv file
        String inputFile = "D:/Arbeit/shared-svn/projects/realLabHH/data/Bahnübergänge/loaded_lcs.csv";
        List<Coord> coordinates = readCsv(inputFile);
        //read in networks and filter for modes car and pt
        String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-network-with-pt.xml.gz";
        Network network = NetworkUtils.readNetwork(networkFile);
        TransportModeNetworkFilter transportModeNetworkFilter = new TransportModeNetworkFilter(network);
        Network ptNetwork = NetworkUtils.createNetwork();
        transportModeNetworkFilter.filter(ptNetwork, new HashSet(Collections.singletonList(TransportMode.pt)));
        Network carNetwork = NetworkUtils.createNetwork();
        transportModeNetworkFilter.filter(carNetwork, new HashSet(Collections.singletonList(TransportMode.car)));

        //TODO @TS CoordSystem Transformation
        CoordinateReferenceSystem test = MGC.getCRS("EPSG:25832");
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", TransformationFactory.WGS84);
        new NetworkTransform(transformation).run(network);
        new NetworkTransform(transformation).run(ptNetwork);


        for (Coord coordinate : coordinates) {
            Link l = NetworkUtils.getNearestLink(carNetwork, coordinate);
            Link nearestPtLink = NetworkUtils.getNearestLink(ptNetwork, coordinate);
            calculateIntersection(l, nearestPtLink);
        }

        EventsManager events = EventsUtils.createEventsManager();
        RailwayCrossingsEventHandler railwayCrossingsEventHandler = new RailwayCrossingsEventHandler(carLinkMap,carLinkTimeMap,ptLinkMap,ptLinkTimeMap );
        events.addHandler(railwayCrossingsEventHandler);
        new MatsimEventsReader(events).readFile(eventFile);

        }


    private static void calculateIntersection(Link carLink, Link ptLink) {

        Coord coordOfIntersection;

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
                connection.put(carLink.getId(), ptLink.getId());

            }

            System.out.println("no match");
        }
    }


    private static void calculateTimeDiff() {
        HashMap<Tuple<Id, Id>, Integer> amountOfCriticalPassings = new HashMap<>();


        for (Id linkId: carLinkTimeMap.keySet()) {
            List<Double> listOfCar = carLinkTimeMap.get(linkId);
            Id correspondingPtLink = connection.get(linkId);
            List <Double> listOfPt = ptLinkTimeMap.get(correspondingPtLink);
            int counter = 0;

            for (double timeCar : listOfCar) {
                for (Double aDouble : listOfPt) {
                    double diff = timeCar - aDouble;
                    if (diff > 0 && diff <= offSet) {
                        counter++;
                    }
                }
                Tuple linkTuple = new Tuple(linkId, correspondingPtLink);
                amountOfCriticalPassings.put(linkTuple, counter);
            }
        }

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
