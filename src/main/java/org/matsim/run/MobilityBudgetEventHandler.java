package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.TransportMode;


import java.util.ArrayList;



public class MobilityBudgetEventHandler implements PersonDepartureEventHandler {


    private static final Logger log = Logger.getLogger(MobilityBudgetEventHandler.class);
    private static ArrayList<Id<Person>> personGotMobilityBudget = new ArrayList<Id<Person>>();
    private static ArrayList<Id<Person>> personUsedCar = new ArrayList<>();

    // in case a more sophisticated implementation is needed
    protected double calculateMobilityBudget (Id<Person> personId ) {
        double mobBudg = 10;
        return mobBudg;
    }

    @Override
    public void reset(int iteration) {
        personUsedCar.clear();
        personGotMobilityBudget.clear();
    }


    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        Id<Person> personId = personDepartureEvent.getPersonId();

        if (RunBaseCaseHamburgScenarioWithMobilityBudget.personsWithMobilityBudget.containsKey(personId)) {

            if (personDepartureEvent.getLegMode().equals(TransportMode.car)) {
                // zero value so if Person already got the mobilityBudget it is removed that way
                RunBaseCaseHamburgScenarioWithMobilityBudget.personsWithMobilityBudget.replace(personId, 0.0);
                log.info(personId + "usesd car");
                personUsedCar.add(personId);
            }

            if (!personDepartureEvent.getLegMode().equals(TransportMode.car) && !personGotMobilityBudget.contains(personId) && !personUsedCar.contains(personId)) {
                RunBaseCaseHamburgScenarioWithMobilityBudget.personsWithMobilityBudget.replace(personId, calculateMobilityBudget(personId));
                log.info("Person: " + personId + "MobilityBudget" + RunBaseCaseHamburgScenarioWithMobilityBudget.personsWithMobilityBudget.get(personId));
                personGotMobilityBudget.add(personId);
            }

            else {
                log.info("");
            }
        }

    }

}
