package org.matsim.parking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.Inject;


/**
 * @author zmeng
 */
public class UtilityBasedParkingPressureEventHandler implements PersonArrivalEventHandler {

    @Inject
    EventsManager eventsManager;
    
    @Inject
    Scenario scenario;

	private final String parkingRelevantTransportMode = TransportMode.car;
	private final String parkTimeLinkAttribute = "parkTime";

    @Override
    public void reset(int iteration) {

    }

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
//		// one approach based on the time
//		if (event.getLegMode().equals(parkingRelevantTransportMode)) {
//			
//			double parkTime = (double) scenario.getNetwork().getLinks().get(event.getLinkId()).getAttributes().getAttribute(parkTimeLinkAttribute);
//			
//			double amount = parkTime / 3600. * (-1. * scenario.getConfig().planCalcScore().getPerforming_utils_hr() 
//					+ scenario.getConfig().planCalcScore().getModes().get(parkingRelevantTransportMode).getMarginalUtilityOfTraveling());
//		    
//			PersonScoreEvent personScoreEvent = new PersonScoreEvent(event.getTime(), event.getPersonId(), amount, "parkPressure");
//		    
//		    eventsManager.processEvent(personScoreEvent);
//		}
		
		// one approach based on the relative scaling of parking pressure
		if (event.getLegMode().equals(parkingRelevantTransportMode)) {
			
			double parkPressure = (double) scenario.getNetwork().getLinks().get(event.getLinkId()).getAttributes().getAttribute(parkTimeLinkAttribute);
			
			double amount = parkPressure * -3.0;
		    
			PersonScoreEvent personScoreEvent = new PersonScoreEvent(event.getTime(), event.getPersonId(), amount, "parkPressure");
		    
		    eventsManager.processEvent(personScoreEvent);
		}
	}
}
