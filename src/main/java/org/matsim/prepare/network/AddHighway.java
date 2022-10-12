package org.matsim.prepare.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

import java.util.List;

public class AddHighway {

	private static final List<String> nodesClockwise = List.of("");
	private static final List<String> nodesCounterClockwise = List.of("", "");

	public static void main(String[] args) {

		var network = NetworkUtils.readNetwork("", ConfigUtils.createConfig());

		for (int i = 1; i < nodesClockwise.size(); i++) {

			var fromNode = network.getNodes().get(Id.createNodeId(nodesClockwise.get(i - 1)));
			var toNode = network.getNodes().get(Id.createNodeId(nodesClockwise.get(i)));

			var link = network.getFactory().createLink(Id.createLinkId("highway_" + i), fromNode, toNode);
			link.setFreespeed(27.7);
			// set other props here
			network.addLink(link);
		}

		// do the same with counter clockwise

		NetworkUtils.writeNetwork(network, "");
	}
}