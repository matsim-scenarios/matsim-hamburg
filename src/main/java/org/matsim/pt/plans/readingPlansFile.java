package org.matsim.pt.plans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

public class readingPlansFile {

    static HashMap<String, MyTransitObject> publicTransit = new HashMap<>();

    public static void main(String[] args) {

        String plansFile = "D:/Arbeit/Hamburg/plans_split/split1.xml";
        String transitSceduleFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_transitSchedule.xml.gz";

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        readTransitScedule(transitSceduleFile, scenario);
        readPlansFile(plansFile, scenario);

        System.out.println("Done");

    }

    private static void readTransitScedule(String transitSceduleFile, Scenario scenario) {
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

        System.out.println("Done");
    }

    private static void readPlansFile(String plansFile, Scenario scenario) {
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansFile);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    continue;
                }
                Leg leg = (Leg) planElement;
                if (!leg.getMode().contains("pt")) {
                    continue;
                }
                Route route = leg.getRoute();


                System.out.println("Done");

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

        void addVehicle (String id) {
            vehicels.add(id);
        }

        public HashMap<String, String> getStations() {
            return stations;
        }

    }

    private static class MyStations implements Comparator {

        final String id;
        final String name;

        MyStations(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int compare(Object o1, Object o2) {
            MyStations station1 = (MyStations) o1;
            MyStations station2 = (MyStations) o2;
            if (station1.id.contains(station2.id)) {
                return 0;
            }
            return station1.id.compareTo(station2.id);
        }
    }
}
