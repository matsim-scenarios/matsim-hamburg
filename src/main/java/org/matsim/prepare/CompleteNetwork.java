/*
package org.matsim.prepare;

import com.google.inject.internal.cglib.transform.$ClassTransformer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.Collection;

public class CompleteNetwork {



    public static void  main (String args []) {

        String shapeNetwork = "D://Gregor//Uni//TUCloud//Masterarbeit//MATSim//input//Freizeitnetz//Freizeitnetz//2021-02-10_Freizeitnetz.shp";
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeNetwork);
        Geometry geometry;
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        ArrayList<MultiLineString> list = new ArrayList<MultiLineString>();

        Network network = scenario.getNetwork();
        Coordinate[] coordinates;

        for (SimpleFeature feature: features) {
            geometry = (Geometry) feature.getDefaultGeometry();
            MultiLineString multiLineString = (MultiLineString) geometry;
            list.add(multiLineString);
        }


        int i = 0;
        while(i <= list.size()) {
            MultiLineString multiLineString = list.get(i);

         
            for (int ii = 0; ii < numberOfLinesStrings; ii++) {
                coordinates = multiLineString.getGeometryN(ii).getCoordinates();
                int iii = 0;
                while (iii +1 < coordinates.length ) {
                    String Coord1 = String.valueOf(coordinates[iii]);
                    String Coord2 = String.valueOf(coordinates[iii+1]);
                    String[] parts = Coord1.split(",");
                    parts[0] = parts[0].replace("(", "");
                    parts[1] = parts[1].replace(")", "");
                    parts[1] = parts[1].replace(" ", "");
                    Coord coord1 = new Coord(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                    parts = Coord2.split(",");
                    parts[0] = parts[0].replace("(", "");
                    parts[1] = parts[1].replace(")", "");
                    parts[1] = parts[1].replace(" ", "");

                    Coord coord2 = new Coord(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));

                    Node n1 = network.getFactory().createNode(Id.createNodeId("N "+ Coord1), coord1);

                    Node n2 = network.getFactory().createNode(Id.createNodeId("N "+ Coord2), coord2);

                    Link link = network.getFactory().createLink(Id.createLinkId("L" + Math.random()), n1, n2);
                    if (!network.getNodes().containsKey(n1.getId())) {
                        network.addNode(n1);
                    }
                    if (!network.getNodes().containsKey(n2.getId())) {
                        network.addNode(n2);
                    }
                    network.addLink(link);

                    iii++;
                }
            }


            i++;
            }
        NetworkWriter writer = new NetworkWriter(network);
        writer.write("D://Gregor//Uni//TUCloud//Masterarbeit//MATSim//input//Freizeitnetz//Freizeitnetz//2021-02-10_Freizeitnetz.xml");
        }



}
*/
