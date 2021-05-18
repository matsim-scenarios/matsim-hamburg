package org.matsim.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;


/**
 * @author zmeng
 */
public class UtilityBasedParkingPressureEventHandler implements PersonArrivalEventHandler {

    @Inject
    EventsManager eventsManager;
    
    @Inject
    Scenario scenario;

	private final String parkingRelevantTransportMode = TransportMode.car;
	static final String parkPressureAttributeName = "parkPressure";

    @Override
    public void reset(int iteration) {

    }

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if (scenario.getPopulation().getPersons().containsKey(event.getPersonId()) && event.getLegMode().equals(parkingRelevantTransportMode)) {

			if(!scenario.getNetwork().getLinks().get(event.getLinkId()).getAttributes().getAsMap().containsKey(parkPressureAttributeName)){
				throw new RuntimeException(parkPressureAttributeName + " is not found as an attribute in link: " + event.getLinkId());
			}
			double parkPressureScore = (double) scenario.getNetwork().getLinks().get(event.getLinkId()).getAttributes().getAttribute(parkPressureAttributeName);

			if (parkPressureScore != 0){

				PersonScoreEvent personScoreEvent = new PersonScoreEvent(event.getTime(), event.getPersonId(), parkPressureScore, "parkPressure");

				eventsManager.processEvent(personScoreEvent);
			}
		}
	}
}
