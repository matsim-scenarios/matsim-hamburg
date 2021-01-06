package org.matsim.pt;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;


public class PersonTest {
	
	private final Id<Person> id;
	private Id<TransitStopFacility> startStation;
//	ArrayList<Id<Vehicle>> vehicleId = new ArrayList<Id<Vehicle>>();
//	ArrayList<Id<TransitStopFacility>> stops = new ArrayList<Id<TransitStopFacility>>();
	private List<PersonTransitUsage> personTransitUsage = new ArrayList<PersonTransitUsage>();
	
//	public ArrayList<Id<TransitStopFacility>> getStops() {
//		return stops;
//	}
//
//	public void addStops(Id<TransitStopFacility> stop) {
//		stops.add(stop);
//	}

	PersonTest(Id<Person> id) {
        this.id = id;
    }
	
	public Id<Person> getId() {
		return id;
	}
	
//	public ArrayList<Id<Vehicle>> getVehicleId() {
//		return vehicleId;
//	}
//
//	public void addVehicles(Id<Vehicle> vehicleId) {
//		this.vehicleId.add(vehicleId);
//	}
	
	public Id<TransitStopFacility> getStartStation() {
		return startStation;
	}

	public void setStartStation(Id<TransitStopFacility> startStation) {
		this.startStation = startStation;
	}
	
	public List<PersonTransitUsage> getPersonTransitUsage(){
		return this.personTransitUsage;
	}
	
	public void addTransitUsage(Id<TransitStopFacility> startStation, Id<TransitStopFacility> endStation, Id<Vehicle> vehicleId) {
		PersonTransitUsage personTransitUsage = new PersonTransitUsage(startStation, endStation, vehicleId);
		this.personTransitUsage.add(personTransitUsage);
	}
	
	public class PersonTransitUsage{
		
		private Id<TransitStopFacility> startStation;
		private Id<TransitStopFacility> endStation;
		private Id<Vehicle> vehicleId;
		
		PersonTransitUsage(Id<TransitStopFacility> startStation, Id<TransitStopFacility> endStation, Id<Vehicle> vehicleId){
			this.endStation = endStation;
			this.startStation = startStation;
			this.vehicleId = vehicleId;
		}
		
		public Id<TransitStopFacility> getStartStation() {
			return startStation;
		}
		public Id<TransitStopFacility> getEndStation() {
			return endStation;
		}
		public Id<Vehicle> getVehicleId() {
			return vehicleId;
		}
		
	}
	
}
