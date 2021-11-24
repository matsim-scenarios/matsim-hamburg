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

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * tried to do this in R but failed at writing proper column names...
 */
public class CutAnalysisCSV2LinkIds {

	public static void main(String[] args) {

		String runBaseDir = "D:/ReallabHH/v2.2/2021-11-12/base-ff/";
		final String runId = "hamburg-v2.0-10pct-base" ;
		String area = "hvvArea"; //set to either 'hvvArea' or 'hhCity'

		String linkId_path = "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-links-in-" +  area + ".tsv";
//		String linkId_path = "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-links-in-" +  area + ".tsv";

		Set<String> csvFiles2Cut = Set.of(runBaseDir + "emission-analysis-hbefa-v4.1-2020/" + runId + ".emissionsPerLink.csv",
				runBaseDir + "emission-analysis-hbefa-v4.1-2020/" + runId + ".emissionsPerLinkPerM.csv",
				runBaseDir + "emission-analysis-hbefa-v4.1-2030/" + runId + ".emissionsPerLinkPerM.csv",
				runBaseDir + "emission-analysis-hbefa-v4.1-2030/" + runId + ".emissionsPerLink.csv",
				runBaseDir + "accidentsAnalysis/ITERS/it.0/" + runId + ".0.accidentCosts_BVWP.csv",
				runBaseDir + "accidentsAnalysis/ITERS/it.0/" + runId + ".0.linkInfo.csv"
		);

		Set<String> links;
		try {
			links = LinksInShp.readLinkIdStrings(linkId_path);
			for (String csvFilePath : csvFiles2Cut) {
				try{
					cutCSV(csvFilePath, area, links);
					System.out.println("processed csv " + csvFilePath);
				} catch (IOException e){
					System.out.println("could not find csv " + csvFilePath);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void cutCSV(String inputCSVPath, String area, Set<String> links) throws IOException {
		//would use CSVReader but we did not use their standard separator and it seems not to be configurable..
		BufferedReader reader = new BufferedReader(new FileReader(inputCSVPath));

		String header = reader.readLine();
		if(!(header.split(";").length > 1)) throw new IllegalStateException("expecting semicolon as separator... aborting");
		header = header.replaceAll("\\s","");

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
	}
}
