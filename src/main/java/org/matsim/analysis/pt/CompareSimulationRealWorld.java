package org.matsim.analysis.pt;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.analysis.pt.PtFromEventsFile.MyTransitObject;
import org.matsim.vehicles.Vehicle;

public class CompareSimulationRealWorld {

	public static void main(String[] args) {

		//Takes from arguments
		String transitScheduleFile = args[0];
		String eventsFile = args[1];
		String realWorldCountsDirectory = args[2];
		String outputResultsDirectory = args[3];
		String outputMissingStationsDirectory = args[4];
		int scalingFactor = Integer.valueOf(args[5]);
		
		
		
		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCountsData = readRealWorldData(
				realWorldCountsDirectory);
		LinkedHashMap<String, String> mapLineToYear = ReadPtCounts.mapLineNoYear(realWorldCountsDirectory);
		HashMap<String, MyTransitObject> transitScheduleData = PtFromEventsFile.readTransitSchedule(transitScheduleFile);
		HashMap<Id<Person>, MyPerson> simResults = PtFromEventsFile.readSimulationData(eventsFile);
		HashMap<Id<Vehicle>, String> vehicleLines = mapVehicleIdToLineNo(transitScheduleData, simResults);
		ArrayList<String> simulationLines = new ArrayList<String>();
		ArrayList<String> simulationLinesMatchingRealWorld = new ArrayList<String>();
		ArrayList<String> missingStationLines = new ArrayList<String>();
		HashMap<String, LinkedHashMap<String, MissingStationPersonCounts>> missingStation = new LinkedHashMap<String, LinkedHashMap<String, MissingStationPersonCounts>>();
		HashMap<String, ArrayList<String>> allSimulationStations = new LinkedHashMap<String, ArrayList<String>>();

		for (Id<Person> key : simResults.keySet()) {

			for (MyPerson.MyTransitUsage personTransitUsage : simResults.get(key).getTransitUsageList()) {
				String lineNo = vehicleLines.get(personTransitUsage.getVehicleId());
				// transit contains all stations and corresponding person counts
				LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(lineNo);
				if (transit != null) {
					if (!simulationLines.contains(lineNo)) {
						simulationLines.add(lineNo);
					}
					if (!simulationLinesMatchingRealWorld.contains(lineNo) && !missingStationLines.contains(lineNo)) {
						simulationLinesMatchingRealWorld.add(lineNo);
					}
					List<String> realworldStops = new ArrayList<String>(transit.keySet());
					String startStation = transitScheduleData.get(lineNo).getStations()
							.get(personTransitUsage.getStartStation());
					String endStation = transitScheduleData.get(lineNo).getStations()
							.get(personTransitUsage.getEndStation());
					if (allSimulationStations.containsKey(lineNo)) {
						if (!allSimulationStations.get(lineNo).contains(startStation)) {
							allSimulationStations.get(lineNo).add(startStation);
						}
						if (!allSimulationStations.get(lineNo).contains(endStation)) {
							allSimulationStations.get(lineNo).add(endStation);
						}

					} else {
						ArrayList<String> values = new ArrayList<String>();
						values.add(startStation);
						values.add(endStation);
						allSimulationStations.put(lineNo, values);
					}

					if (!realworldStops.contains(startStation) || !realworldStops.contains(endStation)) {
						if (startStation.contentEquals("Hamburg Hbf") || startStation.contentEquals("Bf. Harburg")
								&& !realworldStops.contains(startStation)) {
							startStation = "Hauptbahnhof";
						}
						if (endStation.contentEquals("Hamburg Hbf")
								|| endStation.contentEquals("Bf. Harburg") && !realworldStops.contains(endStation)) {
							endStation = "Hauptbahnhof";
						}
					}

					if (realworldStops.contains(startStation) && realworldStops.contains(endStation)) {
						if (realworldStops.indexOf(startStation) < realworldStops.indexOf(endStation)) {
							transit.get(startStation).setEinsteigerOutboundSim();
							transit.get(endStation).setAussteigerOutboundSim();

						} else {
							transit.get(startStation).setEinsteigerInboundSim();
							transit.get(endStation).setAussteigerInboundSim();
						}
					} else {

						if (simulationLinesMatchingRealWorld.contains(lineNo)) {
							simulationLinesMatchingRealWorld.remove(lineNo);
						}
						if (!missingStationLines.contains(lineNo)) {
							missingStationLines.add(lineNo);
						}

						if (realworldStops.contains(startStation)) {
							transit.get(startStation).setEinsteigerSim();
						} else if (realworldStops.contains(endStation)) {
							transit.get(endStation).setAussteigerSim();
						}
						if (!realworldStops.contains(startStation)) {
							if (!missingStation.containsKey(lineNo)) {
								MissingStationPersonCounts missingStationPersonCounts = new MissingStationPersonCounts();
								missingStationPersonCounts.setEinsteigerCount();
								LinkedHashMap<String, MissingStationPersonCounts> stationCounts = new LinkedHashMap<String, MissingStationPersonCounts>();
								stationCounts.put(startStation, missingStationPersonCounts);
								missingStation.put(lineNo, stationCounts);
							} else {
								LinkedHashMap<String, MissingStationPersonCounts> trasit = missingStation.get(lineNo);
								if (trasit.get(startStation) == null) {
									MissingStationPersonCounts missingStationPersonCounts = new MissingStationPersonCounts();
									missingStationPersonCounts.setEinsteigerCount();
									trasit.put(startStation, missingStationPersonCounts);
								} else {
									trasit.get(startStation).setEinsteigerCount();
								}
							}
						}
						if (!realworldStops.contains(endStation)) {
							if (!missingStation.containsKey(lineNo)) {
								MissingStationPersonCounts missingStationPersonCounts = new MissingStationPersonCounts();
								missingStationPersonCounts.setAussteigerCount();
								;
								LinkedHashMap<String, MissingStationPersonCounts> stationCounts = new LinkedHashMap<String, MissingStationPersonCounts>();
								stationCounts.put(endStation, missingStationPersonCounts);
								missingStation.put(lineNo, stationCounts);
							} else {
								LinkedHashMap<String, MissingStationPersonCounts> trasit = missingStation.get(lineNo);
								if (trasit.get(endStation) == null) {
									MissingStationPersonCounts missingStationPersonCounts = new MissingStationPersonCounts();
									missingStationPersonCounts.setAussteigerCount();
									trasit.put(endStation, missingStationPersonCounts);
								} else {
									trasit.get(endStation).setAussteigerCount();
								}
							}
						}

//						System.out.println("Start station end station missing in real world data "
//								+ personTransitUsage.getStartStation().toString() + " and "
//								+ personTransitUsage.getEndStation().toString());
					}
				} 
//				else {
//					System.out.println("Line No: " + lineNo + " not available in real world data");
//				}
			}
		}

		try {
			Writer fwriterResults = new OutputStreamWriter(
					new FileOutputStream(outputResultsDirectory + "/results.csv"), "Windows-1252");
			BufferedWriter bwResults = new BufferedWriter(fwriterResults);
			PrintWriter writerResults = new PrintWriter(bwResults);

			Writer fwriterMissingStations = new OutputStreamWriter(
					new FileOutputStream(outputMissingStationsDirectory + "/missingStations.csv"), "Windows-1252");
			BufferedWriter bwMissingStations = new BufferedWriter(fwriterMissingStations);
			PrintWriter missingStationWriter = new PrintWriter(bwMissingStations);

			//Printing missing stations
			for (String line : realWordCountsData.keySet()) {
				if (missingStationLines.contains(line)) {
					LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(line);
					int realWorldStationCount = transit.keySet().size();
					int simulationCounts = 0;
					for (String station : transit.keySet()) {
						PersonCounts personCounts = transit.get(station);
						simulationCounts += ((personCounts.getAussteigerInboundSim() > 0
								|| personCounts.getAussteigerOutboundSim() > 0
								|| personCounts.getEinsteigerInboundSim() > 0
								|| personCounts.getEinsteigerOutboundSim() > 0 || personCounts.getAussteigerSim() > 0
								|| personCounts.getEinsteigerSim() > 0) ? 1 : 0);
					}
					int missingStationCount = missingStation.get(line).size();
					missingStationWriter.println();
					missingStationWriter.println("Line: " + line + " - Year "+mapLineToYear.get(line));
					missingStationWriter.println("No: of stations in Real world " + realWorldStationCount);
					missingStationWriter
							.println("No: of stations in simulation " + (simulationCounts + missingStationCount));
					missingStationWriter.println();
					missingStationWriter.println("Stations: ");
					missingStationWriter.println();
					missingStationWriter.print("Station");
					missingStationWriter.print(",");
					missingStationWriter.print("Einsteiger Outbound");
					missingStationWriter.print(",");
					missingStationWriter.print("Einsteiger Outbound Sim");
					missingStationWriter.print(",");
					missingStationWriter.print("Aussteiger Outbound");
					missingStationWriter.print(",");
					missingStationWriter.print("Aussteiger Outbound Sim");
					missingStationWriter.print(",");
					missingStationWriter.print("Einsteiger Inbound");
					missingStationWriter.print(",");
					missingStationWriter.print("Einsteiger Inbound Sim");
					missingStationWriter.print(",");
					missingStationWriter.print("Aussteiger Inbound");
					missingStationWriter.print(",");
					missingStationWriter.print("Aussteiger Inbound Sim");
					missingStationWriter.print(",");
					missingStationWriter.print("Einsteiger Sim");
					missingStationWriter.print(",");
					missingStationWriter.print("Aussteiger Sim");
					missingStationWriter.print(",");
					for (String station : transit.keySet()) {
						PersonCounts personCounts = transit.get(station);
						// if() {
						missingStationWriter.println();
						String value = station;
						String stationToPrint = "";
						String[] split = null;
						if (value.contains(",")) {
							split = value.split(",");
							for (int ii = 0; ii < split.length; ii++) {
								stationToPrint += " " + split[ii];
							}
						} else {
							stationToPrint = station;
						}
						missingStationWriter.print(stationToPrint);
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getEinsteigerOutbound());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getEinsteigerOutboundSim());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getAussteigerOutbound());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getAussteigerOutboundSim());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getEinsteigerInbound());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getEinsteigerInboundSim());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getAussteigerInbound());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getAussteigerInboundSim());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getEinsteigerSim());
						missingStationWriter.print(",");
						missingStationWriter.print(personCounts.getAussteigerSim());
						missingStationWriter.print(",");
						// }
					}
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.println("Missing stations in real world: (Available in simulation)");
					missingStationWriter.println();
					missingStationWriter.print("Station");
					missingStationWriter.print(",");
					missingStationWriter.print("Einsteiger");
					missingStationWriter.print(",");
					missingStationWriter.print("Aussteiger");
					LinkedHashMap<String, MissingStationPersonCounts> missingStationCounts = missingStation.get(line);
					for (String station : missingStationCounts.keySet()) {
						String value = station;
						String stationToPrint = "";
						String[] split = null;
						if (value.contains(",")) {
							split = value.split(",");
							for (int ii = 0; ii < split.length; ii++) {
								stationToPrint += " " + split[ii];
							}
						} else {
							stationToPrint = station;
						}
						MissingStationPersonCounts missingStationPersonCounts = missingStationCounts.get(station);
						missingStationWriter.println();
						missingStationWriter.print(stationToPrint);
						missingStationWriter.print(",");
						missingStationWriter.print(missingStationPersonCounts.getEinsteigerCount());
						missingStationWriter.print(",");
						missingStationWriter.print(missingStationPersonCounts.getAussteigerCount());
					}
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.println("Missing stations in simulation: (Available in real world)");
					missingStationWriter.println();
					missingStationWriter.print("Station");
					missingStationWriter.print(",");
					for (String station : transit.keySet()) {
						if (!allSimulationStations.get(line).contains(station)) {
							missingStationWriter.println();
							missingStationWriter.print(station);
						}
					}
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.print("All simulation stations: ");
					missingStationWriter.println();
					missingStationWriter.println();
					for (String station : allSimulationStations.get(line)) {
						missingStationWriter.println();
						String value = station;
						String stationToPrint = "";
						String[] split = null;
						if (value.contains(",")) {
							split = value.split(",");
							for (int ii = 0; ii < split.length; ii++) {
								stationToPrint += " " + split[ii];
							}
						} else {
							stationToPrint = station;
						}
						missingStationWriter.print(stationToPrint);
					}
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.println(
							"==========================================================================================================");
					missingStationWriter.println();
					missingStationWriter.println();
				}

			}

			//Printing results
			for (String line : realWordCountsData.keySet()) {
				if (simulationLinesMatchingRealWorld.contains(line)) {
					LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(line);
					writerResults.println("Line: " + line);
					writerResults.println();
					int i = 0;
					int einsteigerOutboundSim = 0;
					int einsteigerInboundSim = 0;
					DefaultCategoryDataset datasetEinsteiger = new DefaultCategoryDataset();
					DefaultCategoryDataset datasetAussteiger = new DefaultCategoryDataset();
					DefaultCategoryDataset percentageChangeEinsteigerOutbound = new DefaultCategoryDataset();
					DefaultCategoryDataset percentageChangeEinsteigerInbound = new DefaultCategoryDataset();
					for (String station : transit.keySet()) {
						PersonCounts personCounts = transit.get(station);
						if (i == 0) {
							writerResults.print("Station");
							writerResults.print(",");
							writerResults.print("Einsteiger Outbound");
							writerResults.print(",");
							writerResults.print("Einsteiger Outbound Sim");
							writerResults.print(",");
							writerResults.print("Aussteiger Outbound");
							writerResults.print(",");
							writerResults.print("Aussteiger Outbound Sim");
							writerResults.print(",");
							writerResults.print("Einsteiger Inbound");
							writerResults.print(",");
							writerResults.print("Einsteiger Inbound Sim");
							writerResults.print(",");
							writerResults.print("Aussteiger Inbound");
							writerResults.print(",");
							writerResults.print("Aussteiger Inbound Sim");
							writerResults.print(",");
						}
						writerResults.println();
						writerResults.print(station);
						writerResults.print(",");
						writerResults.print(personCounts.getEinsteigerOutbound());
						writerResults.print(",");
						writerResults.print(personCounts.getEinsteigerOutboundSim());
						einsteigerOutboundSim += personCounts.getEinsteigerOutboundSim();
						datasetEinsteiger.addValue(personCounts.getEinsteigerOutboundSim(), "Outbound", station);
						writerResults.print(",");
						writerResults.print(personCounts.getAussteigerOutbound());
						writerResults.print(",");
						writerResults.print(personCounts.getAussteigerOutboundSim());
						datasetAussteiger.addValue(personCounts.getAussteigerOutboundSim(), "Outbound", station);
						writerResults.print(",");
						writerResults.print(personCounts.getEinsteigerInbound());
						writerResults.print(",");
						writerResults.print(personCounts.getEinsteigerInboundSim());
						einsteigerInboundSim += personCounts.getEinsteigerInboundSim();
						datasetEinsteiger.addValue(personCounts.getEinsteigerInboundSim(), "Inbound", station);
						writerResults.print(",");
						writerResults.print(personCounts.getAussteigerInbound());
						writerResults.print(",");
						writerResults.print(personCounts.getAussteigerInboundSim());
						datasetAussteiger.addValue(personCounts.getAussteigerInboundSim(), "Ibound", station);
						writerResults.print(",");
						
						if(personCounts.getEinsteigerOutbound() + personCounts.getEinsteigerOutboundSim() > 0) {
							double scaling = personCounts.getEinsteigerOutboundSim()*scalingFactor;
							double einsteigerOutCount = personCounts.getEinsteigerOutbound();
							double diff = scaling - einsteigerOutCount;
							double sum = scaling + einsteigerOutCount;
							double percChangeEinsteigerOutbound = (diff/sum)*100;
							percentageChangeEinsteigerOutbound.addValue(percChangeEinsteigerOutbound, "Outbound", station);
						}
						if(personCounts.getEinsteigerInbound() + personCounts.getEinsteigerInboundSim() > 0) {
							double scaling = personCounts.getEinsteigerInboundSim()*scalingFactor;
							double einsteigerInCount = personCounts.getEinsteigerInbound();
							double diff = scaling - einsteigerInCount;
							double sum = scaling + einsteigerInCount;
							double percChangeEinsteigerInbound = (diff/sum)*100;
							percentageChangeEinsteigerInbound.addValue(percChangeEinsteigerInbound, "Inbound", station);
						}
						
						i++;
					}
					writerResults.println();
					writerResults.println();
					
					DefaultCategoryDataset datasetTotalPtUsageEinsteiger = new DefaultCategoryDataset();
					datasetTotalPtUsageEinsteiger.addValue(einsteigerOutboundSim, "persons", "Outbound");
					datasetTotalPtUsageEinsteiger.addValue(einsteigerInboundSim, "persons", "Inbound");
					
					generateLineChartDataset(datasetEinsteiger, outputResultsDirectory, line, "Einsteiger");
					generateLineChartDataset(datasetAussteiger, outputResultsDirectory, line, "Aussteiger");
					generateBarChartTotalPtUsage(datasetTotalPtUsageEinsteiger, outputResultsDirectory, line, "BarChart");
					generateBarChartPercentageChange(percentageChangeEinsteigerOutbound, outputResultsDirectory, line, "percentageChangeEinsteigerOutbound");
					generateBarChartPercentageChange(percentageChangeEinsteigerInbound, outputResultsDirectory, line, "percentageChangeEinsteigerInbound");
				}
			}

			writerResults.flush();
			writerResults.close();
			missingStationWriter.flush();
			missingStationWriter.close();

			System.out.println("Written file succesfully");
			System.out.println("size of simulation lines not matching with real world lines " + missingStation.size());
			System.out.println("size of simulation lines " + simulationLines.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> readRealWorldData(
			String realWordCountsFile) {

		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCounts = ReadPtCounts
				.read(realWordCountsFile);

		return realWordCounts;
	}
	
	
	private static HashMap<Id<Vehicle>, String> mapVehicleIdToLineNo(
			HashMap<String, MyTransitObject> transitScheduleData, HashMap<Id<Person>, MyPerson> simResults) {

		HashMap<Id<Vehicle>, String> vehicleLines = new HashMap<Id<Vehicle>, String>();
		for (MyPerson personTest : simResults.values()) {
			for (MyPerson.MyTransitUsage personTransitUsage : personTest.getTransitUsageList()) {
				Id<Vehicle> vehileId = personTransitUsage.getVehicleId();
				for (MyTransitObject transitObject : transitScheduleData.values()) {
					List<String> vehicles = transitObject.getVehicles();
					if (vehicles.contains(vehileId.toString())) {
						vehicleLines.put(vehileId, transitObject.getLine());
					}
				}
			}
		}
		return vehicleLines;
	}
	
	private static void generateLineChartDataset(DefaultCategoryDataset dataset, String outputResultsDirectory,
			String line, String fileName) throws IOException {

		JFreeChart lineChartDatasetEinsteiger = ChartFactory.createLineChart(fileName + " for outbound and Inbound",
				"stops", "No of passengers", dataset, PlotOrientation.HORIZONTAL, true, true, false);

		CategoryPlot lineEinsteigerCategoryPlot = lineChartDatasetEinsteiger.getCategoryPlot();
		lineEinsteigerCategoryPlot.getRenderer().setDefaultSeriesVisibleInLegend(true);

		int lineChartWidth = 1000; /* Width of the image */
		int lineChartHeight = 600; /* Height of the image */
		File directory = new File(outputResultsDirectory + "/" + line);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File lineChartTransitEinsteigerPath = new File(directory + "/transit" + fileName + ".jpeg");
		ChartUtils.saveChartAsJPEG(lineChartTransitEinsteigerPath, lineChartDatasetEinsteiger, lineChartWidth,
				lineChartHeight);

	}
	
	private static void generateBarChartTotalPtUsage(DefaultCategoryDataset dataset, String outputResultsDirectory,
			String line, String fileName) throws IOException {

		JFreeChart totalPtUsagebarChart = ChartFactory.createBarChart(fileName + " for outbound and Inbound", "stops",
				"No of passengers", dataset, PlotOrientation.VERTICAL, true, true, false);

		CategoryPlot categoryPlot = totalPtUsagebarChart.getCategoryPlot();
		BarRenderer barRenderer = (BarRenderer) categoryPlot.getRenderer();
		barRenderer.setMaximumBarWidth(0.10);
		barRenderer.setSeriesPaint(0, Color.BLUE);
		int width = 480; /* Width of the image */
		int height = 480; /* Height of the image */
		File directory = new File(outputResultsDirectory + "/" + line);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File barChartPath = new File(directory + "/TotalPtUsage" + fileName + ".jpeg");
		ChartUtils.saveChartAsJPEG(barChartPath, totalPtUsagebarChart, width, height);

	}
	
	private static void generateBarChartPercentageChange(DefaultCategoryDataset dataset, String outputResultsDirectory,
			String line, String fileName) throws IOException {

		JFreeChart percentageChangebarChart = ChartFactory.createBarChart(fileName + " compared to real world", "stops",
				"No of passengers", dataset, PlotOrientation.HORIZONTAL, true, true, false);

		CategoryPlot categoryPlot = percentageChangebarChart.getCategoryPlot();
		BarRenderer barRenderer = (BarRenderer) categoryPlot.getRenderer();
		barRenderer.setMaximumBarWidth(0.10);
		barRenderer.setSeriesPaint(0, Color.BLUE);
		int width = 900; /* Width of the image */
		int height = 520; /* Height of the image */
		File directory = new File(outputResultsDirectory + "/" + line);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File barChartPath = new File(directory + "/PercentageChange" + fileName + ".jpeg");
		ChartUtils.saveChartAsJPEG(barChartPath, percentageChangebarChart, width, height);

	}

}
