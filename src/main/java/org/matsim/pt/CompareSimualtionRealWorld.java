package org.matsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.MyPerson.MyTransitUsage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.matsim.pt.PtFromEventsFile.*;
import static org.matsim.pt.ReadPtCounts.*;

public class CompareSimualtionRealWorld {

    public static void main(String[] args) {

        String eventsFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_events.xml";
        String transitScheduleFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_transitSchedule.xml.gz";
        String realWordCountsFile = "D:/Arbeit/Hamburg/HVV Fahrgastzahlen 2014-2020";

        LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWorldMap = read(realWordCountsFile);
        HashMap<String, MyTransitObject> scheduleMap = readTransitSchedule(transitScheduleFile);
        HashMap<Id<Person>, MyPerson> simulationMap = readSimulationData(eventsFile);

        int correct = 0;
        int all = 0;
        int diffLine = 0;

        for (MyPerson person : simulationMap.values()) {
            for (MyTransitUsage transitUsage : person.getTransitUsageList()) {
                for (MyTransitObject myTransitObject : scheduleMap.values()) {
                    if (myTransitObject.getVehicels().contains(transitUsage.getVehicleId().toString())) {
                        all++;
                        LinkedHashMap<String, PersonCounts> lineMap = realWorldMap.get("Linie: " + myTransitObject.getLine());
                        if (lineMap == null) {
//                            System.out.println("Unknown line " + myTransitObject.getLine());
                            diffLine++;
                            continue;
                        }
                        boolean inBound = false;
                        boolean outBound = false;
                        String startStation = myTransitObject.getStations().get(transitUsage.getStartStation());
                        String endStation = myTransitObject.getStations().get(transitUsage.getEndStation());
//                        if (startStation == null || endStation == null) {
//                            System.out.println(person.getTransitUsageList());
//                            continue;
//                        }
                        for (String stationName : lineMap.keySet()) {
                            if (stationName == null) {
//                                System.out.println("StationName: " + stationName + ", " + lineMap);
                                continue;
                            }
                            if (stationName.equals(startStation)) {
                                inBound = true;
                                outBound = false;
                                continue;
                            } else if (stationName.equals(endStation)) {
                                inBound = false;
                                outBound = true;
                                continue;
                            }
                        }
                        try {
                            if (!inBound && !outBound) {
                                throw new Exception("Could not find a station name " + startStation + ", " + endStation + ", " + lineMap);
                            } else if (inBound && outBound) {
                                throw new Exception("Unknown error");
                            } else if (inBound) {
                                try {
                                    lineMap.get(startStation).setEinsteigerInboundSim();
                                    lineMap.get(endStation).setAussteigerInboundSim();
                                } catch (NullPointerException e) {
//                                    System.out.println("inBound: start " + startStation + ", end " + endStation + ", " + lineMap);
                                    continue;
                                }
                            } else if (outBound) {
                                try {
                                    lineMap.get(endStation).setAussteigerOutboundSim();
                                    lineMap.get(startStation).setEinsteigerOutboundSim();
                                } catch (NullPointerException e) {
//                                    System.out.println("outBound: start " + startStation + ", end " + endStation + ", " + lineMap);
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        correct++;
                        continue;
                    }
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            System.out.println("---------------------------------------------------------");
        }
        System.out.println("All: " + all);
        System.out.println("Correct: " + correct);
        System.out.println("InCorrect: " + (all - correct));
        System.out.println("DiffLine: " + diffLine);
        System.out.println("Done");

    }


}
