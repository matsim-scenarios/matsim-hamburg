package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.io.*;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zmeng
 */
public class InitialSharingStationsVehiclesGenerator {

    private static final Logger log = Logger.getLogger(InitialSharingStationsVehiclesGenerator.class);

    private static final String NETWORK_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-network-with-pt.xml.gz";

    private static final String SERVICE_AREA = "../../svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_city/hamburg_stadtteil.shp";
    private static final String SWITCH_POINTS_CSV = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hvv-switch-points-2030.csv";

    private final String COORDINATION_SYSTEM = "EPSG:25832";

    private final Network network;
    private String mode;
    private List<SharingStation> sharingStations = new LinkedList<>();
    private String outputPath;

    public InitialSharingStationsVehiclesGenerator(String mode, String outputPath, Network network) {
        this.mode = mode;
        this.outputPath = outputPath;
        this.network = network;
    }


    public static void main(String[] args) throws IOException {
        String coordinationSystem = TransformationFactory.WGS84;

        Network network = prepareNetwork();
        InitialSharingStationsVehiclesGenerator carSharingService = new InitialSharingStationsVehiclesGenerator("scar", "scenarios/input/", network);
        int vehiclesPerStation = 50;
        carSharingService.addStationsFromCSV(SWITCH_POINTS_CSV,coordinationSystem,vehiclesPerStation);
        carSharingService.addStationsRandomlyInNetwork(2000,1);
        carSharingService.write2xmlFile();

        InitialSharingStationsVehiclesGenerator bikeSharingService = new InitialSharingStationsVehiclesGenerator("sbike", "scenarios/input/", network);
        int bikesPerStation = 50;
        bikeSharingService.addStationsFromCSV(SWITCH_POINTS_CSV, coordinationSystem, bikesPerStation);
        bikeSharingService.addStationsRandomlyInNetwork(5000, 1);
        bikeSharingService.write2xmlFile();

        System.out.println("done!!");
    }

    private void write2xmlFile() throws IOException {
        SharingServiceSpecification service = new DefaultSharingServiceSpecification();
        int stationId = 1;

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(this.outputPath + String.format("sharingStationsAndSharingVehicles_%s.csv", this.mode)));
        bufferedWriter.write("link;x;y;capacity");


        for (SharingStation sharingStation : this.sharingStations) {

            Link l = sharingStation.getLink();
            bufferedWriter.newLine();
            bufferedWriter.write(l.getId().toString() +";" + l.getCoord().getX() + ";" + l.getCoord().getY() + ";" + sharingStation.getCapacity());

            ImmutableSharingStationSpecification sharingStationSpecification = ImmutableSharingStationSpecification.newBuilder() //
                    .id(Id.create(mode+"_"+ stationId, org.matsim.contrib.sharing.service.SharingStation.class)) //
                    .capacity(sharingStation.getCapacity()) //
                    .linkId(sharingStation.getLink().getId()) //
                    .build();

            service.addStation(sharingStationSpecification);
            stationId++;

            for (int i = 1; i <= sharingStationSpecification.getCapacity(); i++) {
                String vehicleId = sharingStationSpecification.getId().toString() + "_" + (i);

                service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder() //
                        .id(Id.create(vehicleId, SharingVehicle.class)) //
                        .startStationId(sharingStationSpecification.getId()) //
                        .startLinkId(sharingStationSpecification.getLinkId()) //
                        .build());
            }
        }

        bufferedWriter.close();

        new SharingServiceWriter(service).write(this.outputPath + String.format("sharingStationsAndSharingVehicles_%s.xml", this.mode));
    }


    private static Network prepareNetwork() {
        Network network = NetworkUtils.readNetwork(NETWORK_PATH);

        // remove pt_links and pt_nodes
        var ptLinks = network.getLinks().keySet().stream().filter(linkId -> linkId.toString().contains("pt")).collect(Collectors.toList());
        var ptNodes = network.getNodes().keySet().stream().filter(nodeId -> nodeId.toString().contains("pt")).collect(Collectors.toList());
        ptLinks.forEach(linkId -> network.removeLink(linkId));
        ptNodes.forEach(nodeId -> network.removeNode(nodeId));
        log.info("remove " + ptLinks.size() + " ptlinks");
        log.info("remove " + ptNodes.size() + " ptnodes");
        NetworkUtils.runNetworkCleaner(network);
        return network;
    }

    private void addStationsRandomlyInNetwork(int stationsNum, int vehiclesPerStation) {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        var serviceArea = ShapeFileReader.getAllFeatures(this.SERVICE_AREA);
        List<Geometry> geometries = new LinkedList<>();
        List<Link> links = new LinkedList<>();

        for (SimpleFeature s: serviceArea) {
            Geometry geometry = (Geometry) s.getDefaultGeometry();
            geometries.add(geometry);
        }

        for (Link link : this.network.getLinks().values()) {
            Coord coord = link.getCoord();
            Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
            Point point = geometryFactory.createPoint(coordinate);
            for (Geometry geometry : geometries) {
                if(geometry.contains(point)) {
                    links.add(link);
                    break;
                }
            }
        }

        Collections.shuffle(links);



        for (int i = 0; i < stationsNum; i++) {
            SharingStation sharingStation = new SharingStation(network,links.get(i),vehiclesPerStation);
            this.sharingStations.add(sharingStation);
        }
    }

    private void addStationsFromCSV(String csvFilePath, String transformationSystem, int vehiclesPerStation) throws IOException {

        BufferedReader csvReader = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(csvFilePath));
        //skip first line
        String row = csvReader.readLine();
        CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(transformationSystem, this.COORDINATION_SYSTEM);


        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(";");
            if(data[2].isEmpty() || data[3].isEmpty()) {
                log.info("invalid row: " + row);
                continue;
            }
            double x = Double.parseDouble(data[2]);
            double y = Double.parseDouble(data[3]);

            Coord coord = coordinateTransformation.transform(new Coord(x, y));
            SharingStation sharingStation = new SharingStation(network,coord,vehiclesPerStation);


            sharingStations.add(sharingStation);
        }

        csvReader.close();

    }

    public List<SharingStation> getSharingStations() {
        return sharingStations;
    }

    private static class SharingStation{

        private Network network;
        private Coord coord;
        private Link link;
        private int capacity;

        private SharingStation(Network network, Coord coord, int capacity) {
            this.coord = coord;
            this.network = network;
            this.capacity = capacity;
            this.link = NetworkUtils.getNearestLink(network, coord);
        }

        private SharingStation(Network network, Link link, int capacity){
            this.link = link;
            this.network = network;
            this.capacity = capacity;
            this.coord = link.getCoord();
        }

        public Network getNetwork() {
            return network;
        }

        public Coord getCoord() {
            return coord;
        }

        public Link getLink() {
            return link;
        }

        public int getCapacity() {
            return capacity;
        }
    }
}
