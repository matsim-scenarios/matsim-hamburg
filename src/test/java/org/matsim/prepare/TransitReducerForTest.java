package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zmeng, haowu
 */
public class TransitReducerForTest {

    private static final Logger log = LogManager.getLogger(TransitReducerForTest.class);

    Id<TransitLine> lineId;
    public TransitReducerForTest(Id<TransitLine> lineId) {
        this.lineId = lineId;
    }

    public Scenario reduce(Scenario scenario){

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        TransitLine transitLine = transitSchedule.getTransitLines().get(lineId);

        List<Id<Vehicle>> remainVehicles = new LinkedList<>();

        for (TransitRoute transitRoute : transitLine.getRoutes().values()){
            for(Departure departure :transitRoute.getDepartures().values()){
                remainVehicles.add(departure.getVehicleId());
            }
        }


        Scenario newScenario = ScenarioUtils.createScenario(scenario.getConfig());
        TransitSchedule transitSchedule1 = newScenario.getTransitSchedule();
        transitSchedule1.addTransitLine(transitLine);

        Vehicles transitVehicle = newScenario.getTransitVehicles();
        scenario.getTransitVehicles().getVehicleTypes().values().forEach(transitVehicle::addVehicleType);

        for (Vehicle vehicle: scenario.getTransitVehicles().getVehicles().values()){
            if(remainVehicles.contains(vehicle.getId())){
                Vehicle v = transitVehicle.getFactory().createVehicle(vehicle.getId(), vehicle.getType());
                transitVehicle.addVehicle(v);
            }

        }
        return newScenario;
    }

}
