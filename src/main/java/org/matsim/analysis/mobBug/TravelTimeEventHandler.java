package org.matsim.analysis.mobBug;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;

public class TravelTimeEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {


    private final HashMap<Id<Person>,	Double> departureTimes	= new HashMap<>();
    private final HashMap<Id<Person>,	Double> travelTimes	= new HashMap<>();
    private double travelTimeSum =	0.0;
    private int travelTimeCount =0;


    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {
        if (!personArrivalEvent.getPersonId().toString().contains("pt") && !personArrivalEvent.getPersonId().toString().contains("commercial")) {
            double	departureTime	=	this.departureTimes.get(personArrivalEvent.getPersonId());
            double	travelTime	=	personArrivalEvent.getTime()	-	departureTime;
            this.travelTimeSum	+=	travelTime;
            this.travelTimeCount++;
            this.travelTimes.put(personArrivalEvent.getPersonId(), this.travelTimeSum);
        }

    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if (!personDepartureEvent.getPersonId().toString().contains("pt") && !personDepartureEvent.getPersonId().toString().contains("commercial")) {

            this.departureTimes.put(personDepartureEvent.getPersonId(), personDepartureEvent.getTime());
        }
    }

    public HashMap<Id<Person>, Double> getTravelTimes() {
        return this.travelTimes;
    }






}
