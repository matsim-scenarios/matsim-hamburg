package org.matsim.pt;

import static org.matsim.pt.PtFromEventsFile.readSimulationData;
import static org.matsim.pt.PtFromEventsFile.readTransitSchedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.MyPerson.MyTransitUsage;
import org.matsim.pt.PtFromEventsFile.MyTransitObject;
import org.matsim.vehicles.Vehicle;

public class CompareSimualtionRealWorld {

	public static void main(String[] args) {
		
		String transitScheduleFile = "C:/Users/Aravind/work/Calibration/hh-10pct-19.output_transitSchedule.xml";
		String eventsFile = "C:/Users/Aravind/work/Calibration/hh-10pct-19.output_events.xml";
		String realWordCountsFile = "C:/Users/Aravind/work/Calibration/HVV-counts/HVV Fahrgastzahlen 2014-2020";
		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCountsData = readRealWorldData(
				realWordCountsFile);
		HashMap<String, MyTransitObject> transitScheduleData = readTransitSchedule(transitScheduleFile);
		HashMap<Id<Person>, MyPerson> simResults = readSimulationData(eventsFile); // change
		HashMap<Id<Vehicle>, String> vehicleLines = mapVehicleIdToLineNo(transitScheduleData, simResults);
		ArrayList<String> count = new ArrayList<String>();
		ArrayList<String> count1 = new ArrayList<String>();
		ArrayList<String> missingStationLines = new ArrayList<String>();
		HashMap<String, LinkedHashMap<String, MissingStationPersonCounts>> missingStation = new LinkedHashMap<String, LinkedHashMap<String, MissingStationPersonCounts>>();
		HashMap<String, ArrayList<String>> allSimulationStations = new LinkedHashMap<String, ArrayList<String>>();

		FileWriter fwriterPerson = null;
		try {
			fwriterPerson = new FileWriter(new File("C:/Users/Aravind/work/Calibration" + "/personTransitTest.txt"),
					false);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedWriter bwPerson = new BufferedWriter(fwriterPerson);
		PrintWriter writerPerson = new PrintWriter(bwPerson);

		for (Id<Person> key : simResults.keySet()) {

			for (MyTransitUsage personTransitUsage : simResults.get(key).getTransitUsageList()) {
				String lineNo = vehicleLines.get(personTransitUsage.getVehicleId());
				if (lineNo != null && lineNo.contentEquals("111")) {
					writerPerson.println("Line : " + lineNo);
					writerPerson.println("Person : " + key);
					writerPerson.println("Start : "
							+ transitScheduleData.get(lineNo).getStations().get(personTransitUsage.getStartStation()));
					writerPerson.println("End : "
							+ transitScheduleData.get(lineNo).getStations().get(personTransitUsage.getEndStation()));
				}
			}

			for (MyTransitUsage personTransitUsage : simResults.get(key).getTransitUsageList()) {
				String lineNo = vehicleLines.get(personTransitUsage.getVehicleId());
				// transit contains all stations and corresponding person counts
				LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(lineNo);
				if (transit != null) {
					if (!count.contains(lineNo)) {
						count.add(lineNo);
					}
					if (!count1.contains(lineNo) && !missingStationLines.contains(lineNo)) {
						count1.add(lineNo);
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
						
						if (count1.contains(lineNo)) {
							count1.remove(lineNo);
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
				} else {
					System.out.println("Line No: " + lineNo + " not available in real world data");
				}
			}
		}

		writerPerson.flush();
		writerPerson.close();

		try {
			FileWriter fwriter = new FileWriter(new File("C:/Users/Aravind/work/Calibration" + "/results.csv"), false);
			BufferedWriter bw = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bw);

			FileWriter fwriter1 = new FileWriter(new File("C:/Users/Aravind/work/Calibration" + "/missingStations.csv"),
					false);
			BufferedWriter bw1 = new BufferedWriter(fwriter1);
			PrintWriter missingStationWriter = new PrintWriter(bw1);

			FileWriter fwriter2 = new FileWriter(new File("C:/Users/Aravind/work/Calibration" + "/simulationLines.txt"),
					false);
			BufferedWriter bw2 = new BufferedWriter(fwriter2);
			PrintWriter writer2 = new PrintWriter(bw2);

			FileWriter fwriter3 = new FileWriter(
					new File("C:/Users/Aravind/work/Calibration" + "/missingLinesInSimulation.txt"), false);
			BufferedWriter bw3 = new BufferedWriter(fwriter3);
			PrintWriter writer3 = new PrintWriter(bw3);
			
			FileWriter fwriter4 = new FileWriter(
					new File("C:/Users/Aravind/work/Calibration" + "/listAfterRemoval.txt"), false);
			BufferedWriter bw4 = new BufferedWriter(fwriter4);
			PrintWriter writer4 = new PrintWriter(bw4);
			
			FileWriter fwriter5 = new FileWriter(
					new File("C:/Users/Aravind/work/Calibration" + "/missingStationLines.txt"), false);
			BufferedWriter bw5 = new BufferedWriter(fwriter5);
			PrintWriter writer5 = new PrintWriter(bw5);

			Set<String> realWorldLines = realWordCountsData.keySet();

			for (String line : realWorldLines) {
				if (!count.contains(line)) {
					writer3.println(line);
				}
			}

			for (String line : count) {
				writer2.println(line);
			}
			for (String line : count1) {
				writer4.println(line);
			}
			for (String line : missingStationLines) {
				writer5.println(line);
			}

			for (String line : realWordCountsData.keySet()) {
				if (missingStationLines.contains(line)) {
					LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(line);
					int realWorldStationCount = transit.keySet().size();
					int simulationCounts = 0;
					for (String station : transit.keySet()) {
						PersonCounts personCounts = transit.get(station);
						simulationCounts += ((personCounts.getAussteigerInboundSim() > 1
								|| personCounts.getAussteigerOutboundSim() > 1
								|| personCounts.getEinsteigerInboundSim() > 1
								|| personCounts.getEinsteigerOutboundSim() > 1 || personCounts.getAussteigerSim() > 1
								|| personCounts.getEinsteigerSim() > 1) ? 1 : 0);
					}
					int missingStationCount = missingStation.get(line).size();
					missingStationWriter.println();
					missingStationWriter.println("Line: " + line);
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
						missingStationWriter.print(station);
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
						MissingStationPersonCounts missingStationPersonCounts = missingStationCounts.get(station);
						missingStationWriter.println();
						missingStationWriter.print(station);
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
						missingStationWriter.print(station);
					}
					missingStationWriter.println();
					missingStationWriter.println();
					missingStationWriter.println(
							"==========================================================================================================");
					missingStationWriter.println();
					missingStationWriter.println();
				}

			}

			for (String line : realWordCountsData.keySet()) {
				if (count1.contains(line)) {
					LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(line);
					writer.println("Line: " + line);
					writer.println();
					int i = 0;
					for (String station : transit.keySet()) {
						PersonCounts personCounts = transit.get(station);
						if (i == 0) {
							writer.print("Station");
							writer.print(",");
							writer.print("Einsteiger Outbound");
							writer.print(",");
							writer.print("Einsteiger Outbound Sim");
							writer.print(",");
							writer.print("Aussteiger Outbound");
							writer.print(",");
							writer.print("Aussteiger Outbound Sim");
							writer.print(",");
							writer.print("Einsteiger Inbound");
							writer.print(",");
							writer.print("Einsteiger Inbound Sim");
							writer.print(",");
							writer.print("Aussteiger Inbound");
							writer.print(",");
							writer.print("Aussteiger Inbound Sim");
							writer.print(",");
						}
						writer.println();
						writer.print(station);
						writer.print(",");
						writer.print(personCounts.getEinsteigerOutbound());
						writer.print(",");
						writer.print(personCounts.getEinsteigerOutboundSim());
						writer.print(",");
						writer.print(personCounts.getAussteigerOutbound());
						writer.print(",");
						writer.print(personCounts.getAussteigerOutboundSim());
						writer.print(",");
						writer.print(personCounts.getEinsteigerInbound());
						writer.print(",");
						writer.print(personCounts.getEinsteigerInboundSim());
						writer.print(",");
						writer.print(personCounts.getAussteigerInbound());
						writer.print(",");
						writer.print(personCounts.getAussteigerInboundSim());
						writer.print(",");

						i++;
					}
					writer.println();
					writer.println();
				}
			}

			writer.flush();
			writer.close();
			missingStationWriter.flush();
			missingStationWriter.close();
			writer2.flush();
			writer2.close();
			writer3.flush();
			writer3.close();
			writer4.flush();
			writer4.close();
			writer5.flush();
			writer5.close();

			System.out.println("Written file succesfully");
			System.out.println("size of simulation lines not matching with real world lines " + missingStation.size());
			System.out.println("size of simulation lines " + count.size());
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

	private static HashMap<Id<Vehicle>, String> mapVehicleIdToLineNo(HashMap<String, MyTransitObject> transitScheduleData,
			HashMap<Id<Person>, MyPerson> simResults) { // change

		HashMap<Id<Vehicle>, String> vehicleLines = new HashMap<Id<Vehicle>, String>();
		for (MyPerson personTest : simResults.values()) {
			for (MyTransitUsage personTransitUsage : personTest.getTransitUsageList()) {
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

}
