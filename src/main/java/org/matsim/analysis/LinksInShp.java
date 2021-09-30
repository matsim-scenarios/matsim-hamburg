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

package org.matsim.analysis;

import com.opencsv.CSVWriter;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opencsv.ICSVWriter.*;

public class LinksInShp {

	public static void main(String[] args) {

//		String hamburgHVVShape = "../../svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_hvv/hamburg_hvv.shp";
		String hamburgCityShape = "../../svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_city/hamburg_stadtteil.shp";


		List<PreparedGeometry> hamburgHVVGeoms = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(hamburgCityShape));
		Network hamburgNetwork = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-with-pt.xml.gz");

		writeLinksInShpCSV(hamburgNetwork, hamburgHVVGeoms, "../../svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-links-in-hhCity.tsv");
	}


	static void writeLinksInShpCSV(Network network, List<PreparedGeometry> preparedGeometryList, String outputFilePath){
		if(! outputFilePath.endsWith(".tsv")) throw new IllegalArgumentException("output file path should end with .tsv");

		Set<String[]> linkIds = network.getLinks().values().stream()
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), preparedGeometryList))
				.map(link -> new String[]{link.getId().toString()})
				.collect(Collectors.toSet());

		try {
			CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath), '\t', NO_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
			writer.writeNext(new String[]{"linkId"});
			writer.writeAll(linkIds);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
