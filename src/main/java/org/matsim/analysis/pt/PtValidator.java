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

    HashMap<Id<Person>, PtPassenger> ptUsageMap = new HashMap<>();
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
                PtPassenger ptPassenger = ptUsageMap.get(personId);
                if (ptPassenger.isUsingPt()) {
                    try {
                        throw new Exception("already using pt");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ptPassenger.setUsingPt(true);
                Id<TransitStopFacility> startStation = movingVehicle.get(vehicleId);
                ptPassenger.setStartStation(startStation);
            } else {
                PtPassenger ptPassenger = new PtPassenger(personId);
                ptUsageMap.put(personId, ptPassenger);
                ptPassenger.setUsingPt(true);
                Id<TransitStopFacility> startStation = movingVehicle.get(vehicleId);
                ptPassenger.setStartStation(startStation);
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
            PtPassenger ptPassenger = ptUsageMap.get(personId);
            if (!ptPassenger.isUsingPt()) {
                try {
                    throw new Exception("should already using pt");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Id<TransitStopFacility> endStation = movingVehicle.get(vehicleId);
            ptPassenger.addTransitUsage(ptPassenger.getStartStation(), endStation, vehicleId);
            ptPassenger.setUsingPt(false);
        }
    }

    public HashMap<Id<Person>, PtPassenger> getPtUsageMap() {
        return ptUsageMap;
    }
}
