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


import com.opencsv.CSVReader;
import org.w3c.dom.css.Counter;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * tried to do this in R but failed at writing proper column names...
 */
public class CutAnalysisCSV2LinkIds {

	public static void main(String[] args) {

		String inputCSVPath = "D:/ReallabHH/v2.2/basierendAufP2-3-5/output-speeded-sharing10pct-hamburg-v2.2-reallabHH2030/emission-analysis-hbefa-v4.1-2020/hamburg-v2.0-10pct-reallab2030.emissionsPerLink.csv";

		String area = "hvvArea"; //set to either 'hvvArea' or 'hhCity'
		String linkId_path = "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-links-in-" +  area + ".tsv";

		Set<String> links;
		try {
			links = LinksInShp.readLinkIdStrings(linkId_path);
			//would use CSVReader but we did not use their standard separator and it seems not to be configurable..
			BufferedReader reader = new BufferedReader(new FileReader(inputCSVPath));

			String header = reader.readLine();
			if(!(header.split(";").length > 1)) throw new IllegalStateException("expecting semicolon as separator... aborting");

			Set<String> filteredDataSets = new HashSet<>();


			int originalLines = 0;
			String line = reader.readLine();
			while (line != null){
				originalLines++;
				if (links.contains(line.split(";")[0])){
					filteredDataSets.add(line);
				}
				line = reader.readLine();
			}
			reader.close();

			System.out.println("original data set size = " + originalLines);
			System.out.println("filtered data set size = " + filteredDataSets.size());

			String outputFilePath = inputCSVPath.substring(0, inputCSVPath.lastIndexOf(".") ) + "-" + area + ".csv";
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
			writer.write(header);

			for (String filteredDataSet : filteredDataSets) {
				writer.newLine();
				writer.write(filteredDataSet);
			}

			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}