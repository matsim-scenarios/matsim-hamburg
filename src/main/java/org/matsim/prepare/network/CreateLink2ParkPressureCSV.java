/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.prepare.network;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import one.util.streamex.StreamEx;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

/**
 *
 * i actually was looking for this class but could not find, so had to reimplement it. :(
 * maybe because of data confidentiality. but here, no data is published....
 * tschlenther, 30.08.2021
 */
public class CreateLink2ParkPressureCSV {

	/**
	 * TODO see shared-svn/projects/realLabHH/data/Parkdruckdaten*
	 */
	private static final String INPUT_SHAPE_FILE = "D:/svn/shared-svn/projects/realLabHH/data/Parkdruckdaten* ....";

	/**
	 *
	 */
	private static final String INPUT_NETWORK = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt.xml.gz";
	//TODO you need to change this to your local copy path, because the IDE is not allowed to write here
	private static final String OUTPUT_CSV = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt.link2ParkPressure.csv";

	public static void main(String[] args) {


		Collection<SimpleFeature> polygons = ShapeFileReader.getAllFeatures(IOUtils.resolveFileOrResource(INPUT_SHAPE_FILE));
		Network network = NetworkUtils.readNetwork(INPUT_NETWORK);

//		CoordinateTransformation tf = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:25832");
		Map<? extends Link, Double> link2Polygon = StreamEx.of(network.getLinks().values())
				.mapToEntry(link -> link, link -> polygons.stream()
						.filter(g -> isLinkInGeometry(link, g))
						.findFirst()
						.orElse(null))
				.filterValues(Objects::nonNull)
				.mapValues(p ->  (Double) (p.getAttribute("Parkpl")))
				.filterValues(pressure -> pressure < 1.0)
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		try {
			CSVWriter writer = new CSVWriter(new FileWriter(OUTPUT_CSV), ',', ICSVWriter.NO_QUOTE_CHARACTER, '"', "\n");
			writer.writeNext(new String[]{"link_id","Parkpl"});
			link2Polygon.entrySet()
					.forEach(entry -> writer.writeNext(new String[]{entry.getKey().getId().toString(), entry.getValue().toString()}));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private static boolean isLinkInGeometry(Link link, SimpleFeature g, CoordinateTransformation tf) {
		return ((Geometry)g.getDefaultGeometry()).contains(MGC.coord2Point(tf.transform(link.getCoord()))) ;
	}

	private static boolean isLinkInGeometry(Link link, SimpleFeature g) {
		return ((Geometry)g.getDefaultGeometry()).contains(MGC.coord2Point(link.getCoord())) ;
	}

}
