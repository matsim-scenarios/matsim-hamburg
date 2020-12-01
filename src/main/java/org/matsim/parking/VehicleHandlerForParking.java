package org.matsim.parking;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zmeng
 */
public class VehicleHandlerForParking implements VehicleHandler {

    QSim qsim;

    public VehicleHandlerForParking(QSim qSim) {
        this.qsim = qSim;
    }

    private Map<QVehicle, Double> vehicle2parkWishTime = new HashMap<>();

    @Override
    public void handleVehicleDeparture(QVehicle qVehicle, Link link) {

    }

    @Override
    public boolean handleVehicleArrival(QVehicle qVehicle, Link link) {
        //todo assign park time to link
        double time = this.qsim.getSimTimer().getTimeOfDay();
        if (!this.vehicle2parkWishTime.containsKey(qVehicle)) {
            this.vehicle2parkWishTime.put(qVehicle, time + (Double) link.getAttributes().getAsMap().get("parkTime"));
        }
        boolean letVehicleStop = time >= this.vehicle2parkWishTime.get(qVehicle);
        if(letVehicleStop)
            this.vehicle2parkWishTime.remove(qVehicle);
        return letVehicleStop;
    }

    @Override
    public void handleInitialVehicleArrival(QVehicle qVehicle, Link link) {

    }
}
