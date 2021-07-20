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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RailwayCrossings {

    public static HashMap<Id, Double> carLinkMap = new HashMap<>();
    public static HashMap<Id, List<Double>> carLinkTimeMap = new HashMap<>();
    public static HashMap<Id, Double> ptLinkMap = new HashMap<>();
    public static HashMap<Id, List<Double>> ptLinkTimeMap = new HashMap<>();
    public static HashMap<Id, Id> connection = new HashMap<>();
    public static double offSet = 1.0;

    public static void main(String[] args) throws Exception {

        //read input csv file
        String inputFile = "C:/Users/Gregor/Documents/shared-svn/projects/realLabHH/data/Bahnübergänge/loaded_lcs.csv";
        List<Coord> coordinates = readCsv(inputFile);
        //read in networks and filter for modes car and pt
        String networkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-network-with-pt.xml.gz";
        Network network = NetworkUtils.readNetwork(networkFile);
        TransportModeNetworkFilter transportModeNetworkFilter = new TransportModeNetworkFilter(network);
        Network ptNetwork = NetworkUtils.createNetwork();
        transportModeNetworkFilter.filter(ptNetwork, new HashSet(Collections.singletonList(TransportMode.pt)));
        Network carNetwork = NetworkUtils.createNetwork();
        transportModeNetworkFilter.filter(carNetwork, new HashSet(Collections.singletonList(TransportMode.car)));
        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:25832");
        List<Coord> transformedCoord = new ArrayList<>();

        for (Coord coordinate : coordinates) {
            coordinate = transformation.transform(coordinate);
            transformedCoord.add(coordinate);
            Link l = NetworkUtils.getNearestLink(carNetwork, coordinate);
            Link nearestPtLink = NetworkUtils.getNearestLink(ptNetwork, coordinate);
            carLinkMap.put(l.getId(),0.0);
            ptLinkMap.put(nearestPtLink.getId(), 0.0);
            connection.put(l.getId(), nearestPtLink.getId());
            //calculateIntersection(l, nearestPtLink);
       }

        writeCoord2CSV(coordinates, transformedCoord);

        EventsManager events = EventsUtils.createEventsManager();
        RailwayCrossingsEventHandler railwayCrossingsEventHandler = new RailwayCrossingsEventHandler(carLinkMap,carLinkTimeMap,ptLinkMap,ptLinkTimeMap );
        events.addHandler(railwayCrossingsEventHandler);
        String eventFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-10pct/output/hamburg-v1.1-10pct.output_events.xml.gz";
        new MatsimEventsReader(events).readFile(eventFile);
        HashMap<Tuple<Id<Link>, Id<Link>>, Integer> criticalPassings = calculateTimeDiff();
        writeResults2CSV(criticalPassings);
        }


    public static void calculateIntersection(Link carLink, Link ptLink) {

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
            System.out.println("lines are parallel");
        } else {
            double x = (b2 * c1 - b1 * c2) / det;
            double y = (a1 * c2 - a2 * c1) / det;
            //check if intersection is on the link
            if (carLink.getFromNode().getCoord().getX() < x && x < carLink.getToNode().getCoord().getX() && carLink.getFromNode().getCoord().getY() < y && y < carLink.getToNode().getCoord().getY() ) {
                coordOfIntersection = new Coord(x,y);
                double distance1 = NetworkUtils.getEuclideanDistance(carLink.getFromNode().getCoord(), coordOfIntersection);
                carLinkMap.put(carLink.getId(), distance1);
                double distance2 = NetworkUtils.getEuclideanDistance(ptLink.getFromNode().getCoord(), coordOfIntersection);
                ptLinkMap.put(ptLink.getId(), distance2);
                connection.put(carLink.getId(), ptLink.getId());
            }
            else {
                System.out.println("no match");
            }
        }
    }


    public static HashMap<Tuple<Id<Link>, Id<Link>>, Integer> calculateTimeDiff() {
        HashMap<Tuple<Id<Link>, Id<Link>>, Integer> amountOfCriticalPassings = new HashMap<>();


        for (Id<Link> linkId: carLinkTimeMap.keySet()) {
            List<Double> listOfCar = carLinkTimeMap.get(linkId);
            Id correspondingPtLink = connection.get(linkId);
            List <Double> listOfPt = ptLinkTimeMap.get(correspondingPtLink);
            int counter = 0;


            if (listOfCar != null && listOfPt!= null  ) {
                if (listOfCar.size()> listOfPt.size()) {
                    //System.out.println("Fall 1 Auto länger als pt ");
                    for (Double ptTime : listOfPt) {
                        for (Double carTime: listOfCar) {
                            //System.out.println(ptTime);
                            //System.out.println(carTime);
                            double diff = ptTime - carTime;
                            System.out.println(diff);
                            if (diff > 0 && diff <= offSet) {
                                counter++;
                            }
                        }
                    }
                    Tuple linkTuple = new Tuple(linkId, correspondingPtLink);
                    amountOfCriticalPassings.put(linkTuple, counter);
                }

                if (listOfPt.size()> listOfCar.size()) {
                    //System.out.println("Fall 2 pt länger als auto ");

                    for (double timeCar : listOfCar) {
                        for (Double aDouble : listOfPt) {
                            System.out.println(timeCar);
                            System.out.println(listOfPt);
                            double diff = timeCar - aDouble;
                            System.out.println(diff);
                            if (diff > 0 && diff <= offSet) {
                                counter++;
                            }
                        }
                    }
                    Tuple linkTuple = new Tuple(linkId, correspondingPtLink);
                    amountOfCriticalPassings.put(linkTuple, counter);
                }

            }
        }
        return amountOfCriticalPassings;


    }

    private static List<Coord> readCsv(String fileName) throws Exception {
        List<Coord> coordsOfCrossings = new ArrayList<>();
        CSVParser csvParser = new CSVParserBuilder().withSeparator(',').build();
        // if your csv file doesn't have header line, remove withSkipLines(1)
        try (CSVReader reader = new CSVReaderBuilder(
                new FileReader(fileName)).withCSVParser(csvParser).withSkipLines(1).build()) {
                List<String[]> r = reader.readAll();
                for (int i = 0; i<r.size(); i++) {
                    String[] test = r.get(i);
                    Coord coord = new Coord(Double.parseDouble(test[4]), Double.parseDouble(test[3]));
                    coordsOfCrossings.add(coord);
                }
            return coordsOfCrossings;
        }
    }


    public static void writeResults2CSV (HashMap<Tuple<Id<Link>, Id<Link>>, Integer> criticalPassings ) throws IOException {
        FileWriter writer = new FileWriter("D://Arbeit//Network_RSV_Edited.xml");
        writer.write("carLink"+";"+"ptLink"+";"+"amountOfCrossings");
        writer.append("\n");

        for (Tuple<Id<Link>, Id<Link>> i: criticalPassings.keySet()) {
            writer.append(i.getFirst().toString()+";"+i.getSecond().toString()+";"+criticalPassings.get(i));
            writer.append("\n");
        }
        writer.close();
    }

    private static void writeCoord2CSV (List<Coord> originalCoordList, List<Coord> transformedCoordList ) throws IOException {
        FileWriter writer = new FileWriter("C:/Users/Gregor/Documents/VSP_Arbeit/test.csv");
        writer.write("originalX"+","+"originalY"+","+"transX"+","+"transY");
        writer.append("\n");

        for (int i = 0; i < originalCoordList.size(); i++) {
            writer.append(originalCoordList.get(i).getX()+","+originalCoordList.get(i).getY()+","+transformedCoordList.get(i).getX()+","+transformedCoordList.get(i).getY());
            writer.append("\n");
        }
        writer.close();
    }
}
