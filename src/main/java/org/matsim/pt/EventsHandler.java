package org.matsim.pt;

import java.util.HashMap;

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

public class EventsHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler  {

	HashMap<Id<Vehicle>, Id<TransitStopFacility>> movingVehicle = new HashMap<>();
	HashMap<Id<Person>, PersonTest> ptUsageMap = new HashMap<>();
	final Id<TransitStopFacility> tripEndsStopId = Id.create(0, TransitStopFacility.class);
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		
		Id<TransitStopFacility> facilityId = event.getFacilityId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		movingVehicle.put(vehicleId, facilityId);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		Id<Person> personId = event.getPersonId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		if(personId.toString().contains("pt")) {
			return;
		}
		if(vehicleId.toString().contains("tr")) {
			if(ptUsageMap.containsKey(personId)) {
				PersonTest person = ptUsageMap.get(personId);
//				person.addStops(movingVehicle.get(vehicleId));
//				person.addStops(tripEndsStopId);
//				person.addVehicles(vehicleId);
				person.addTransitUsage(person.getStartStation(), movingVehicle.get(vehicleId), vehicleId);
			}else {
				 try {
	                    throw new Exception("should already using pt");
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
			}
			
		}
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		
		Id<Person> personId = event.getPersonId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		if(personId.toString().contains("pt")) {
			return;
		}
		if(vehicleId.toString().contains("tr")) {
			if(ptUsageMap.containsKey(personId)) {
				PersonTest person = ptUsageMap.get(personId);
				//person.addStops(movingVehicle.get(vehicleId));
				person.setStartStation(movingVehicle.get(vehicleId));
				//person.addVehicles(vehicleId);
			}else {
				PersonTest person = new PersonTest(personId);
				//person.addStops(movingVehicle.get(vehicleId));
				person.setStartStation(movingVehicle.get(vehicleId));
				//person.addVehicles(vehicleId);
				ptUsageMap.put(personId, person);
			}
			
		}
		
	
		
	}
	 public HashMap<Id<Person>, PersonTest> getPtUsageMap() {
	        return ptUsageMap;
	    }
}
