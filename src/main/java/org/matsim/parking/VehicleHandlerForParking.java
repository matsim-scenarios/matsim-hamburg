//package org.matsim.parking;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.core.mobsim.qsim.QSim;
//import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
//import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * @author zmeng
// */
//public class VehicleHandlerForParking implements VehicleHandler {
//
//    private static Logger logger = Logger.getLogger(VehicleHandlerForParking.class);
//
//    private final QSim qsim;
//    private final Scenario scenario;
//
//    public VehicleHandlerForParking(QSim qSim, Scenario scenario) {
//        this.qsim = qSim;
//        this.scenario = scenario;
//    }
//
//    private Map<QVehicle, Double> vehicle2parkWishTime = new ConcurrentHashMap<>();
//
//
//
//    @Override
//    public void handleVehicleDeparture(QVehicle qVehicle, Link link) {
//
//    }
//
//    @Override
//    public boolean handleVehicleArrival(QVehicle qVehicle, Link link) {
//
//        String parkAttribute = "parkTime";
//        if(link.getAttributes().getAsMap().containsKey(parkAttribute) &&
//                (Double)link.getAttributes().getAttribute(parkAttribute) > 0 &&
//                scenario.getPopulation().getPersons().containsKey(qVehicle.getDriver().getId())){
//
//            double time = this.qsim.getSimTimer().getTimeOfDay();
//            if (!this.vehicle2parkWishTime.containsKey(qVehicle)) {
//
//                double allowedParkingTime = Math.min(time + (Double) link.getAttributes().getAsMap().get(parkAttribute),
//                        scenario.getConfig().qsim().getEndTime().seconds());
//
//                this.vehicle2parkWishTime.put(qVehicle, allowedParkingTime);
//                //logger.info("At time (" + time + ") person with Id (" + qVehicle.getDriver().getId() + ") submit park request at link(" + link.getId() + "). " +
//                //"its waiting time should be " + link.getAttributes().getAttribute(parkAttribute) + ", now the number of waiting car is: " + this.vehicle2parkWishTime.size());
//            }
//
//            boolean letVehicleStop = (time >= this.vehicle2parkWishTime.get(qVehicle));
//            if(letVehicleStop){
//                this.vehicle2parkWishTime.remove(qVehicle);
//                //logger.info("At time (" + time + ") person with Id (" + qVehicle.getDriver().getId() + ") finish parking" + ", now the number of waiting car is: " + this.vehicle2parkWishTime.size());
//            }
//            return letVehicleStop;
//        } else
//            return true;
//    }
//
//    @Override
//    public void handleInitialVehicleArrival(QVehicle qVehicle, Link link) {
//    }
//}
