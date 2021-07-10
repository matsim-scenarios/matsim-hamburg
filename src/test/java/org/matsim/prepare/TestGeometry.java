package org.matsim.prepare;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.RailwayCrossings;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;


public class TestGeometry {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() {

        Network net = NetworkUtils.createNetwork();

        //create two links who cross
        Node fromNode1 = NetworkUtils.createNode(Id.createNodeId("testNode1"), new Coord(0,0));
        Node toNode1 = NetworkUtils.createNode(Id.createNodeId("testNode2"), new Coord(3,3));
        Node fromNode2 = NetworkUtils.createNode(Id.createNodeId("testNode3"), new Coord(1,0));
        Node toNode2 = NetworkUtils.createNode(Id.createNodeId("testNode4"), new Coord(1,3));
        Link l1 = NetworkUtils.createLink(Id.createLinkId("tesLink1"),fromNode1, toNode1, net, NetworkUtils.getEuclideanDistance(fromNode1.getCoord(), toNode1.getCoord()),0.0,0.0,0);
        Link l2 = NetworkUtils.createLink(Id.createLinkId("tesLink2"),fromNode2, toNode2, net, NetworkUtils.getEuclideanDistance(fromNode2.getCoord(), toNode2.getCoord()),0.0,0.0,0);
        net.addNode(fromNode1);
        net.addNode(fromNode2);
        net.addNode(toNode1);
        net.addNode(toNode2);
        net.addLink(l1);
        net.addLink(l2);
        RailwayCrossings.calculateIntersection(l1, l2);

        //create two links who don´t cross
        Node fromNode3 = NetworkUtils.createNode(Id.createNodeId("testNode5"), new Coord(0,0));
        Node toNode3 = NetworkUtils.createNode(Id.createNodeId("testNode6"), new Coord(3,3));
        Node fromNode4 = NetworkUtils.createNode(Id.createNodeId("testNode7"), new Coord(1,0));
        Node toNode4 = NetworkUtils.createNode(Id.createNodeId("testNode8"), new Coord(4,3));
        Link l3 = NetworkUtils.createLink(Id.createLinkId("tesLink3"),fromNode3, toNode3, net, NetworkUtils.getEuclideanDistance(fromNode3.getCoord(), toNode3.getCoord()),0.0,0.0,0);
        Link l4 = NetworkUtils.createLink(Id.createLinkId("tesLink4"),fromNode4, toNode4, net, NetworkUtils.getEuclideanDistance(fromNode4.getCoord(), toNode4.getCoord()),0.0,0.0,0);
        net.addNode(fromNode3);
        net.addNode(fromNode4);
        net.addNode(toNode3);
        net.addNode(toNode4);
        net.addLink(l3);
        net.addLink(l4);
        RailwayCrossings.calculateIntersection(l3, l4);

        //create two links who cross but not on the link itself
        Node fromNode5 = NetworkUtils.createNode(Id.createNodeId("testNode9"), new Coord(0,0));
        Node toNode5 = NetworkUtils.createNode(Id.createNodeId("testNode10"), new Coord(3,3));
        Node fromNode6 = NetworkUtils.createNode(Id.createNodeId("testNode11"), new Coord(6,0));
        Node toNode6 = NetworkUtils.createNode(Id.createNodeId("testNode12"), new Coord(6,3));
        Link l5 = NetworkUtils.createLink(Id.createLinkId("tesLink5"),fromNode5, toNode5, net, NetworkUtils.getEuclideanDistance(fromNode5.getCoord(), toNode5.getCoord()),0.0,0.0,0);
        Link l6 = NetworkUtils.createLink(Id.createLinkId("tesLink6"),fromNode6, toNode6, net, NetworkUtils.getEuclideanDistance(fromNode6.getCoord(), toNode6.getCoord()),0.0,0.0,0);
        net.addNode(fromNode5);
        net.addNode(fromNode6);
        net.addNode(toNode5);
        net.addNode(toNode6);
        net.addLink(l5);
        net.addLink(l6);
        RailwayCrossings.calculateIntersection(l5, l6);

    }
}
