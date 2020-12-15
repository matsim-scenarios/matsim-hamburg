package org.matsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PtFromEventsFile {

    static HashMap<String, MyTransitObject> publicTransit = new HashMap<>();

    public static void main(String[] args) {

        String eventsFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_events.xml";
        String transitScheduleFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_transitSchedule.xml.gz";

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        readTransitSchedule(transitScheduleFile, scenario);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        MyEventHandler myEventHandler = new MyEventHandler();
        eventsManager.addHandler(myEventHandler);
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        eventsManager.initProcessing();
        reader.readFile(eventsFile);
        eventsManager.finishProcessing();

        HashMap<Id<Person>, MyPerson> ptUsage = myEventHandler.getPtUsageMap();

        System.out.println("Done");

    }

    private static void readTransitSchedule(String transitSceduleFile, Scenario scenario) {
        TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
        transitScheduleReader.readFile(transitSceduleFile);

        for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
            String id[] = transitLine.getId().toString().split("-");
            MyTransitObject myTransitObject = new MyTransitObject(id[0]);
            publicTransit.put(id[0], myTransitObject);
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    String stationId = transitRouteStop.getStopFacility().getId().toString();
                    String name = transitRouteStop.getStopFacility().getName();
                    myTransitObject.addStation(stationId, name);
                }
                for (Departure departure : transitRoute.getDepartures().values()) {
                    myTransitObject.addVehicle(departure.getVehicleId().toString());
                }
            }
        }
    }

    private static class MyTransitObject {

        final String line;
        HashMap<String, String> stations = new HashMap();
        List<String> vehicels = new ArrayList<>();

        MyTransitObject(String line) {
            this.line = line;
        }

        void addStation(String id, String name) {
            if (!stations.containsKey(id)) {
                stations.put(id, name);
            }
        }

        void addVehicle(String id) {
            vehicels.add(id);
        }

        public HashMap<String, String> getStations() {
            return stations;
        }

    }

}

