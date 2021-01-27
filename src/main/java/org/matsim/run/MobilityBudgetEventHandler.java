package org.matsim.run;

import com.graphhopper.jsprit.core.problem.solution.route.activity.DeliverService;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import java.util.ArrayList;


public class MobilityBudgetEventHandler implements PersonEntersVehicleEventHandler, PersonMoneyEventHandler {

    private static ArrayList<Id<Person>> personGotMobilityBudget = new ArrayList<Id<Person>>();


    // in case a more sophisticated implementation is needed
    protected double calculateMobilityBudget (Person p ) {
        double mobBudg = (double) p.getAttributes().getAttribute("mobilityBudget");
        return mobBudg;
    }


    //&& RunBaseCaseHamburgScenarioWithMobilityBudget.personsWithMobilityBudget.contains()

    @Override
    public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {
        Id<Person> pId = personEntersVehicleEvent.getPersonId();
        if (personEntersVehicleEvent.getVehicleId().toString().startsWith("tr") && !personGotMobilityBudget.contains(pId) ) {

           for(Person person: RunBaseCaseHamburgScenarioWithMobilityBudget.personsWithMobilityBudget) {
               if(person.getId().equals(pId)) {
                   double sum = calculateMobilityBudget(person);
                   PersonMoneyEvent moneyEvent = new PersonMoneyEvent(personEntersVehicleEvent.getTime(), personEntersVehicleEvent.getPersonId(), sum, "mobilityBudget", null);
                   personGotMobilityBudget.add(person.getId());
               }
            }
        }
        else {
            // nothing to Do here unless we have intermodal Routing??
        }

    }

    @Override
    public void handleEvent(PersonMoneyEvent personMoneyEvent) {
        personMoneyEvent.getPersonId();



    }
}
