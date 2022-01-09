/*
package org.matsim.analysis.mobBug;


import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class Network2Shape {
    private final static Logger log = Logger.getLogger(Network2Shape.class);
    private final static String inputNetworkFile ="D://Gregor//Uni//TUCloud//Masterarbeit//MATSim//input//hamburg-v1.1-network-with-pt.xml.gz";
    private final static String outputDirectory ="D://Gregor//Uni//TUCloud//Masterarbeit//Analysen//";
    private final static String crs ="EPSG:25832";

    public static void  main(String args[]) {

        Network net = NetworkUtils.readNetwork(inputNetworkFile);
        exportNetwork2Shp(outputDirectory,crs, net);

    }

    public static void exportNetwork2Shp(String outputDirectory, String crs, Network net){

        String outputPath = outputDirectory + "network-shp/";
        File file = new File(outputPath);
        file.mkdirs();

        PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
                .setCrs(MGC.getCRS(crs))
                .setName("Link")
                .addAttribute("Id", String.class)
                .addAttribute("Length", Double.class)
                .addAttribute("capacity", Double.class)
                .addAttribute("lanes", Double.class)
                .addAttribute("Freespeed", Double.class)
                .addAttribute("Modes", String.class)
                .create();

        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (Link link : net.getLinks().values()){
            if (link.getAllowedModes().contains(TransportMode.pt)) {
                SimpleFeature feature = factory.createPolyline(
                        new Coordinate[]{
                                new Coordinate(MGC.coord2Coordinate((link.getFromNode().getCoord()))),
                                new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord()))
                        }, new Object[] {link.getId(), link.getLength(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(), link.getAllowedModes()
                        }, null
                );
                features.add(feature);
            }
        }

        log.info("Writing network to shapefile... ");
        ShapeFileWriter.writeGeometries(features, outputPath + "network.shp");
        log.info("Writing network to shapefile... Done.");
    }

}*/
