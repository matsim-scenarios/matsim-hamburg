package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;

public class MobilityBudgetEventHandlerV2  implements PersonDepartureEventHandler {

    private static ArrayList<Id<Person>> personGotMobilityBudget = new ArrayList<>();
    private static ArrayList<Id<Person>> personUsedCar = new ArrayList<>();


    @Override
    public void reset(int iteration) {
        personUsedCar.clear();
        personGotMobilityBudget.clear();
    }


    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        Id<Person> personId = personDepartureEvent.getPersonId();

        if (RunBaseCaseWithMobilityBudgetV2.personsWithMobilityBudget.contains(personId)) {


            if (personDepartureEvent.getLegMode().equals(TransportMode.car)) {
                // zero value so if Person already got the mobilityBudget it is removed that way
                RunBaseCaseWithMobilityBudgetV2.personsWithMobilityBudget.remove(personId);
                personUsedCar.add(personId);
            }

            if (!personDepartureEvent.getLegMode().equals(TransportMode.car) && !personGotMobilityBudget.contains(personId) && !personUsedCar.contains(personId)) {
                //RunBaseCaseWithMobilityBudgetV2.personsWithMobilityBudget.replace(personId, calculateMobilityBudget(personId));
                personGotMobilityBudget.add(personId);
            }
        }

    }
}
