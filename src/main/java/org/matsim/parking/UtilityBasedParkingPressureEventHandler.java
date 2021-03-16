package org.matsim.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;


/**
 * @author zmeng
 */
public class UtilityBasedParkingPressureEventHandler implements PersonLeavesVehicleEventHandler {

    @Inject
    EventsManager eventsManager;

    @Override
    public void handleEvent(PersonLeavesVehicleEvent personLeavesVehicleEvent) {
        double amount = 0;
        PersonScoreEvent personScoreEvent = new PersonScoreEvent(personLeavesVehicleEvent.getTime(), personLeavesVehicleEvent.getPersonId(),amount,"");

    }

    @Override
    public void reset(int iteration) {

    }
}
