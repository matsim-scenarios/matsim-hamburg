package org.matsim.pt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PersonTest.PersonTransitUsage;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ReadEvents {

	public static void main(String[] args) {
		
		String transitScheduleFile = "C:/Users/Aravind/work/Calibration/hh-10pct-19.output_transitSchedule.xml";
		String eventsFile = "C:/Users/Aravind/work/Calibration/hh-10pct-19.output_events.xml";
		String realWordCountsFile = "C:/Users/Aravind/work/Calibration/HVV-counts/HVV Fahrgastzahlen 2014-2020";
		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCountsData = readRealWorldData(realWordCountsFile);
		HashMap<String, TransitObject> transitScheduleData = readTrasitSchedule(transitScheduleFile);
		HashMap<Id<Person>, PersonTest> simResults = readSimulationData(eventsFile);
		HashMap<Id<Vehicle>, String> vehicleLines = mapVehicleIdToLineNo(transitScheduleData, simResults);
		Iterator<Id<Vehicle>> vehicleLinesItr = vehicleLines.keySet().iterator();
//		while(vehicleLinesItr.hasNext()) {
//			Id<Vehicle> id = vehicleLinesItr.next();
//			System.out.println("-----> "+id+" "+vehicleLines.get(id));
//		}
		
		for(Id<Person> key : simResults.keySet()) {
			
			for(PersonTransitUsage personTransitUsage : simResults.get(key).getPersonTransitUsage()) {
				String lineNo = vehicleLines.get(personTransitUsage.getVehicleId());
				LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get("Linie: "+lineNo);
				if(transit != null) {
					List<String> array = new ArrayList<String>(transit.keySet());
					String startStation = transitScheduleData.get(lineNo).getStops().get(personTransitUsage.getStartStation());
					String endStation = transitScheduleData.get(lineNo).getStops().get(personTransitUsage.getEndStation());
				if(array.contains(startStation)&&array.contains(endStation)) {
				if(array.indexOf(startStation) < array.indexOf(endStation)) {
					transit.get(startStation).setEinsteigerOutboundSim();
					transit.get(endStation).setAussteigerOutboundSim();
					
				}else {
					transit.get(startStation).setEinsteigerInboundSim();
					transit.get(endStation).setAussteigerInboundSim();
				}
			}else {
				System.out.println("Start station end station missing in real world data "+personTransitUsage.getStartStation().toString()+" and "+personTransitUsage.getEndStation().toString());
			}
			}else {
				System.out.println("Line No: "+lineNo+" not available in real world data");
			}
			}
		}
		
		
		try {
			FileWriter fwriter = new FileWriter(new File("C:/Users/Aravind/work/Calibration" + "/results.txt"), false);
			BufferedWriter bw = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bw);

					for(String line : realWordCountsData.keySet()) {
						LinkedHashMap<String, PersonCounts> transit = realWordCountsData.get(line);
						writer.println("Line: "+line);
						writer.println();
						for(String station : transit.keySet()) {
							PersonCounts personCounts = transit.get(station);
						
							writer.println("Station: "+station+"--- Einsteiger Outbound: "+personCounts.getEinsteigerOutbound()+"--- Einsteiger Outbound Sim: "+personCounts.getEinsteigerOutboundSim()+"--- Aussteiger Outbound: "+personCounts.getAussteigerOutbound()+"--- Aussteiger Outbound Sim: "+personCounts.getAussteigerOutboundSim()+"--- Einsteiger Inbound: "+personCounts.getEinsteigerInbound()+"--- Einsteiger Inbound Sim: "+personCounts.getEinsteigerInboundSim()+"--- Aussteiger Inbound: "+personCounts.getAussteigerInbound()+"--- Aussteiger Inbound Sim: "+personCounts.getAussteigerInboundSim());
						}
						writer.println();
						writer.print("==========================================================================================================");
					}		

			writer.flush();
			writer.close();
			System.out.println("Written file succesfully");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	 public static LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> readRealWorldData(String realWordCountsFile) {

		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCounts = ReadPtCounts.read(realWordCountsFile);
		
		return realWordCounts;
	 }
	 
	 public static HashMap<Id<Person>, PersonTest> readSimulationData(String eventsFile) {
	        EventsManager eventsManager = EventsUtils.createEventsManager();
	        EventsHandler eventHandler = new EventsHandler();
	        eventsManager.addHandler(eventHandler);
	        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	        eventsManager.initProcessing();
	        reader.readFile(eventsFile);
	        eventsManager.finishProcessing();

	        System.out.println("Done, reading simulation data");

	        return  eventHandler.getPtUsageMap();
	    }
	
	public static HashMap<String, TransitObject> readTrasitSchedule(String transitScheduleFile) {
		
		HashMap<String, TransitObject> publicTransit = new HashMap<>();
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(transitScheduleFile);
		
		for(TransitLine transitLines : scenario.getTransitSchedule().getTransitLines().values()) {
			String[] routeId = transitLines.getId().toString().split("-");
			TransitObject transitObject = new TransitObject(routeId[0]);
			publicTransit.put(routeId[0], transitObject);
			for(TransitRoute route : transitLines.getRoutes().values()) {
				for(TransitRouteStop transitStops : route.getStops()) {
					TransitStopFacility stopFacility = transitStops.getStopFacility();
					Id<TransitStopFacility> stopFacilityId = stopFacility.getId();
					String stopFacilityName = stopFacility.getName();
					transitObject.setStops(stopFacilityId, stopFacilityName);
				}
				for(Departure departures : route.getDepartures().values()) {
					Id<Vehicle> vehicleId = departures.getVehicleId();
					transitObject.addVehicles(vehicleId.toString());
				}
			}
			
		}
		System.out.println("done");
		return publicTransit;
	}
	
	private static HashMap<Id<Vehicle>, String> mapVehicleIdToLineNo(HashMap<String, TransitObject> transitScheduleData, HashMap<Id<Person>, PersonTest> simResults) {
		
		HashMap<Id<Vehicle>, String> vehicleLines = new HashMap<Id<Vehicle>, String>();
		for(PersonTest personTest : simResults.values()) {
			for(PersonTransitUsage personTransitUsage : personTest.getPersonTransitUsage()) {
				Id<Vehicle> vehileId = personTransitUsage.getVehicleId();
				for(TransitObject transitObject : transitScheduleData.values()) {
					ArrayList<String> vehicles = transitObject.getVehicles();
					if(vehicles.contains(vehileId.toString())) {
						vehicleLines.put(vehileId, transitObject.getLine());
					}
				}
			}
		}
		return vehicleLines;
	}
	
	static class TransitObject {
		
		private final String line;
		private HashMap<Id<TransitStopFacility>, String> stops = new HashMap<Id<TransitStopFacility>, String>();
		ArrayList<String> vehicles = new ArrayList<String>();
		
		public ArrayList<String> getVehicles() {
			return vehicles;
		}

		public void addVehicles(String vehicleId) {
			vehicles.add(vehicleId);
		}

		public HashMap<Id<TransitStopFacility>, String> getStops() {
			return stops;
		}

		public void setStops(Id<TransitStopFacility> stopFacilityId, String stopFacilityName) {
			if(!stops.containsKey(stopFacilityId)) {
				stops.put(stopFacilityId, stopFacilityName);
			}
		}

		TransitObject(String line){
			this.line = line;
		}

		public String getLine() {
			return line;
		}
		
	}
}


