package org.matsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class MyPerson {

    private final Id<Person> id;
    private boolean usingPt = false;
    private Id<TransitStopFacility> startStation;
    private List<MyTransitUsage> transitUsageList = new ArrayList<>();

    MyPerson(Id<Person> id) {
        this.id = id;
    }

    public void setStartStation(Id<TransitStopFacility> startStation) {
        this.startStation = startStation;
    }

    public Id<TransitStopFacility> getStartStation() {
        Id<TransitStopFacility> startStation = this.startStation;
        this.startStation = null;
        return startStation;
    }

    public void addTransitUsage(Id<TransitStopFacility> startStation, Id<TransitStopFacility> endStation, Id<Vehicle> vehicleId) {
        MyTransitUsage transitUsage = new MyTransitUsage(startStation, endStation, vehicleId);
        transitUsageList.add(transitUsage);
    }

    public boolean isUsingPt() {
        return usingPt;
    }

    public void setUsingPt(boolean usingPt) {
        this.usingPt = usingPt;
    }

    private class MyTransitUsage {

        private final Id<TransitStopFacility> startStation;
        private final Id<TransitStopFacility> endStation;
        private final Id<Vehicle> vehicleId;

        MyTransitUsage (Id<TransitStopFacility> startStation, Id<TransitStopFacility> endStation, Id<Vehicle> vehicleId) {
            this.startStation = startStation;
            this.endStation = endStation;
            this.vehicleId = vehicleId;
        }

    }
}
