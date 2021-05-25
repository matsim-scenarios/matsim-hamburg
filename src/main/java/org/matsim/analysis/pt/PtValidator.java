package org.matsim.analysis.pt;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;

public class PtValidator implements VehicleArrivesAtFacilityEventHandler,  PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

    HashMap<Id<Person>, MyPerson> ptUsageMap = new HashMap<>();
    HashMap<Id<Vehicle>, Id<TransitStopFacility>> movingVehicle = new HashMap<>();

    @Override
    public void reset(int iteration) {
        ptUsageMap.clear();
        movingVehicle.clear();
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<TransitStopFacility> facilityId = event.getFacilityId();
        movingVehicle.put(vehicleId, facilityId);
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Person> personId = event.getPersonId();
        Id<Vehicle> vehicleId = event.getVehicleId();
        if (personId.toString().contains("pt")) {
            return;
        }
        if (vehicleId.toString().contains("tr")) {
            if (ptUsageMap.containsKey(personId)) {
                MyPerson myPerson = ptUsageMap.get(personId);
                if (myPerson.isUsingPt()) {
                    try {
                        throw new Exception("already using pt");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                myPerson.setUsingPt(true);
                Id<TransitStopFacility> startStation = movingVehicle.get(vehicleId);
                myPerson.setStartStation(startStation);
            } else {
                MyPerson myPerson = new MyPerson(personId);
                ptUsageMap.put(personId, myPerson);
                myPerson.setUsingPt(true);
                Id<TransitStopFacility> startStation = movingVehicle.get(vehicleId);
                myPerson.setStartStation(startStation);
            }
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Id<Person> personId = event.getPersonId();
        Id<Vehicle> vehicleId = event.getVehicleId();
        if (personId.toString().contains("pt")) {
            return;
        }
        if (vehicleId.toString().contains("tr")) {
            MyPerson myPerson = ptUsageMap.get(personId);
            if (!myPerson.isUsingPt()) {
                try {
                    throw new Exception("should already using pt");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Id<TransitStopFacility> endStation = movingVehicle.get(vehicleId);
            myPerson.addTransitUsage(myPerson.getStartStation(), endStation, vehicleId);
            myPerson.setUsingPt(false);
        }
    }

    public HashMap<Id<Person>, MyPerson> getPtUsageMap() {
        return ptUsageMap;
    }
}
