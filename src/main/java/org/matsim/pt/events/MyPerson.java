package org.matsim.pt.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class MyPerson {

    final Id<Person> id;
    boolean usingPt = false;
    Id<TransitStopFacility> startStation;
    List<MyTransitUsage> transitUsageList = new ArrayList<>();

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

        final Id<TransitStopFacility> startStation;
        final Id<TransitStopFacility> endStation;
        final Id<Vehicle> vehicleId;

        MyTransitUsage (Id<TransitStopFacility> startStation, Id<TransitStopFacility> endStation, Id<Vehicle> vehicleId) {
            this.startStation = startStation;
            this.endStation = endStation;
            this.vehicleId = vehicleId;
        }

    }
}
