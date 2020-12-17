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
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PtFromEventsFile {

    public static HashMap<Id<Person>, MyPerson> readSimulationData(String eventsFile) {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        MyEventHandler myEventHandler = new MyEventHandler();
        eventsManager.addHandler(myEventHandler);
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        eventsManager.initProcessing();
        reader.readFile(eventsFile);
        eventsManager.finishProcessing();

        System.out.println("Done, reading simulation data");

        return  myEventHandler.getPtUsageMap();
    }

    static HashMap<String, MyTransitObject> readTransitSchedule(String transitSceduleFile) {
        HashMap<String, MyTransitObject> publicTransit = new HashMap<>();

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
        transitScheduleReader.readFile(transitSceduleFile);

        for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
            String id[] = transitLine.getId().toString().split("-");
            MyTransitObject myTransitObject = new MyTransitObject(id[0]);
            publicTransit.put(id[0], myTransitObject);
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    Id<TransitStopFacility> stationId = transitRouteStop.getStopFacility().getId();
                    String name = transitRouteStop.getStopFacility().getName();
                    myTransitObject.addStation(stationId, name);
                }
                for (Departure departure : transitRoute.getDepartures().values()) {
                    myTransitObject.addVehicle(departure.getVehicleId().toString());
                }
            }
        }

        System.out.println("Done, reading schedule data");

        return publicTransit;
    }

    static class MyTransitObject {

        private final String line;
        private HashMap<Id<TransitStopFacility>, String> stations = new HashMap();
        private List<String> vehicels = new ArrayList<>();

        MyTransitObject(String line) {
            this.line = line;
        }

        void addStation(Id<TransitStopFacility> id, String name) {
            if (!stations.containsKey(id)) {
                stations.put(id, name);
            }
        }

        void addVehicle(String id) {
            vehicels.add(id);
        }

        public HashMap<Id<TransitStopFacility>, String> getStations() {
            return stations;
        }

        public String getLine() {
            return line;
        }

        public List<String> getVehicels() {
            return vehicels;
        }
    }

}

