package org.matsim.escooter;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zmeng
 */
public class EScooterCharger implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    private Map<Id<Person>, PersonDepartureEvent> eScooterUsers = new HashMap<>();

    @Inject
    EventsManager eventsManager;

    @Inject
    EScooterConfigGroup eScooterConfigGroup;

    @Override
    public void reset(int iteration) {
        this.eScooterUsers.clear();
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        if(personArrivalEvent.getLegMode().equals(eScooterConfigGroup.getMode())){
            if(!this.eScooterUsers.containsKey(personArrivalEvent.getPersonId())){
                throw new RuntimeException("no departure event for person: "+ personArrivalEvent.getPersonId() + " is found. Can not calculate trip fares with e-scooter");
            } else {
                double depTime = this.eScooterUsers.get(personArrivalEvent.getPersonId()).getTime();
                double arrTime = personArrivalEvent.getTime();

                double faresInTime = Math.ceil((arrTime-depTime)/60) * eScooterConfigGroup.getFarePerMin();
                double amount = - ( faresInTime + eScooterConfigGroup.getBaseFare() );
                eventsManager.processEvent(new PersonMoneyEvent(arrTime,personArrivalEvent.getPersonId(), amount,"eScooter","eScooter"));
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if (personDepartureEvent.getLegMode().equals(eScooterConfigGroup.getMode())){
            eScooterUsers.put(personDepartureEvent.getPersonId(),personDepartureEvent);
        }
    }
}
