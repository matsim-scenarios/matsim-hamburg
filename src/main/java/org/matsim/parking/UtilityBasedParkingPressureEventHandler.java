package org.matsim.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.run.RunSharingScenario;

import java.util.Set;


/**
 * @author zmeng
 */
public class UtilityBasedParkingPressureEventHandler implements PersonArrivalEventHandler {

    @Inject
    EventsManager eventsManager;
    
    @Inject
    Scenario scenario;

	private final Set<String> parkingRelevantTransportModes = Set.of(TransportMode.car, RunSharingScenario.SHARING_CAR_MODE);
	public static final String PARK_PRESSURE_ATTRIBUTE_NAME = "parkPressure";

    @Override
    public void reset(int iteration) {

    }

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if (scenario.getPopulation().getPersons().containsKey(event.getPersonId()) && parkingRelevantTransportModes.contains(event.getLegMode())) {

			if(!scenario.getNetwork().getLinks().get(event.getLinkId()).getAttributes().getAsMap().containsKey(PARK_PRESSURE_ATTRIBUTE_NAME)){
				throw new RuntimeException(PARK_PRESSURE_ATTRIBUTE_NAME + " is not found as an attribute in link: " + event.getLinkId());
			}
			double parkPressureScore = (double) scenario.getNetwork().getLinks().get(event.getLinkId()).getAttributes().getAttribute(PARK_PRESSURE_ATTRIBUTE_NAME);

			if (parkPressureScore != 0){

				PersonScoreEvent personScoreEvent = new PersonScoreEvent(event.getTime(), event.getPersonId(), parkPressureScore, "parkPressure");

				eventsManager.processEvent(personScoreEvent);
			}
		}
	}
}
