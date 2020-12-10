package org.matsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;
import java.util.List;

public class MyPerson {

    final Id<Person> id;
    boolean isUsingPt = false;
    String startStation;
    List<MyTransitUsage> transitUsageList = new ArrayList<>();

    MyPerson(Id<Person> id) {
        this.id = id;
    }

    public void setStartStation(String startStation) {
        this.startStation = startStation;
    }

    public void addTransitUsage(String startStation, String endStation, String vehicleId) {
        MyTransitUsage transitUsage = new MyTransitUsage(startStation, endStation, vehicleId);
        transitUsageList.add(transitUsage);
    }

    private class MyTransitUsage {

        final String startStation;
        final String endStation;
        final String vehicleId;

        MyTransitUsage (String startStation, String endStation, String vehicleId) {
            this.startStation = startStation;
            this.endStation = endStation;
            this.vehicleId = vehicleId;
        }

    }
}
