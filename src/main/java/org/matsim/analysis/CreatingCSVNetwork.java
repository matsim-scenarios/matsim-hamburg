package org.matsim.analysis;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import org.matsim.core.utils.io.IOUtils;


import java.io.BufferedWriter;


public class CreatingCSVNetwork {

    public static void main(String[] args) {

        String networkFile = "D://Gregor//Uni//TUCloud//Masterarbeit//MATSim//input//hamburg-v1.0-network-with-pt.xml//hamburg-v1.0-network-with-pt.xml.gz";


        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        networkReader.readFile(networkFile);




/*
        for(Link link : scenario.getNetwork().getLinks().values()) {

            if (link.getAllowedModes().contains("car")) {
                scenario.getNetwork().removeLink(link.getId());
                scenario.getNetwork().removeNode(link.getFromNode().getId());
                scenario.getNetwork().removeNode(link.getToNode().getId());
            }

        }
*/


        BufferedWriter writer = IOUtils.getBufferedWriter("edge.txt");
        try {
            writer.write("id,from,to,x,y,wkt");
            writer.newLine();
            for (Link link : scenario.getNetwork().getLinks().values()) {

                    if (link.getId().toString().contains("pt")) {
                        writer.write(link.getId() + ",");
                        writer.write(link.getFromNode().getId() + ",");
                        writer.write(link.getToNode().getId() + "," );
                        writer.write(link.getCoord().getX() + "," );
                        writer.write(link.getCoord().getY() + ",");
                        writer.write("\"LINESTRING (" + link.getFromNode().getCoord().getX() + " " +  link.getFromNode().getCoord().getY() + ", ");
                        writer.write(link.getToNode().getCoord().getX() + " " +  link.getToNode().getCoord().getY() + ")\",");
                        int count = 0;
                        writer.write(count + "");
                        writer.newLine();
                        writer.flush();
                    }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        BufferedWriter writer2 = IOUtils.getBufferedWriter("node.txt");
        try {
            writer2.write("id;x;y");
            writer2.newLine();
            for (Link link : scenario.getNetwork().getLinks().values()) {

                    Node fromNode = link.getFromNode();
                    Node toNode = link.getToNode();
                    if (fromNode.getId().toString().contains("pt")) {
                        writer2.write(fromNode.getId() + ";");
                        writer2.write(fromNode.getCoord().getX() + ";" );
                        writer2.write(fromNode.getCoord().getY() + "");
                        writer2.newLine();
                        writer2.flush();
                    }
                    if (toNode.getId().toString().contains("pt")) {
                        writer2.write(toNode.getId() + ";");
                        writer2.write(toNode.getCoord().getX() + ";" );
                        writer2.write(toNode.getCoord().getY() + "");
                        writer2.newLine();
                        writer2.flush();
                    }
            }
            writer2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }




}
