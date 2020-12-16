package org.matsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.MyPerson.MyTransitUsage;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.matsim.pt.PtFromEventsFile.*;
import static org.matsim.pt.ReadPtCounts.*;

public class CompareSimualtionRealWorld {

    public static void main(String[] args) throws Exception {

        String eventsFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_events.xml";
        String transitScheduleFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_transitSchedule.xml.gz";
        String realWordCountsFile = "D:/Arbeit/Hamburg/HVV Fahrgastzahlen 2014-2020";

        LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWorldMap = read(realWordCountsFile);
        HashMap<Id<Person>, MyPerson> siumulationMap = readSimulationData(eventsFile);
        HashMap<String, MyTransitObject> scheduleMap = readTransitSchedule(transitScheduleFile);

       for (MyPerson person : siumulationMap.values()) {
           for (MyTransitUsage transitUsage : person.getTransitUsageList()) {
               for (MyTransitObject myTransitObject : scheduleMap.values()) {
                   if (myTransitObject.getVehicels().contains(transitUsage.getVehicleId())) {
                       LinkedHashMap<String, PersonCounts> lineMap = realWorldMap.get(myTransitObject.getLine());
                       if (lineMap == null) {
                           System.out.println("Unknown line " + myTransitObject.getLine());
                           continue;
                       }
                       boolean inBound = false;
                       boolean outBound = false;
                       String startStation = myTransitObject.getStations().get(transitUsage.getStartStation());
                       String endStaiotn = myTransitObject.getStations().get(transitUsage.getEndStation());
                       for (String staiotnName : lineMap.keySet()) {
                           if (staiotnName.equals(startStation)) {
                               inBound = true;
                               outBound = false;
                               continue;
                           } else if (staiotnName.equals(endStaiotn)) {
                               inBound = false;
                               outBound = true;
                               continue;
                           }
                       }
                       if (inBound || outBound) {
                           throw new Exception("Could not find a station name");
                       } else if (inBound) {
                           lineMap.get(startStation).setEinsteigerInboundSim();
                           lineMap.get(endStaiotn).setAussteigerInboundSim();
                       } else if (outBound) {
                           lineMap.get(startStation).setEinsteigerOutboundSim();
                           lineMap.get(endStaiotn).setAussteigerOutboundSim();
                       } else {
                           throw new Exception("Unknown error");
                       }
                   }
               }
           }
       }

        System.out.println("Done");

    }


}
