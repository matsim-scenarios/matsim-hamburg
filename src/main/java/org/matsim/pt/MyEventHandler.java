package org.matsim.pt;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;

import java.util.HashMap;

public class MyEventHandler implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityEndEventHandler {

    HashMap<Id<Person>, MyPerson> ptUsageMap = new HashMap<>();


    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {

    }

    @Override
    public void handleEvent(VehicleDepartsAtFacilityEvent event) {

    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getVehicleId().toString().contains("tr") || !ptUsageMap.containsKey(event.getPersonId())) {
            MyPerson myPerson = new MyPerson(event.getPersonId());
            ptUsageMap.put(event.getPersonId(), myPerson);
            myPerson.setStartStation(event.getAttributes().get("facility"));
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {

    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        System.out.println("sss");
    }
}
