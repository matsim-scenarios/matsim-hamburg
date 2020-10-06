package org.matsim.hamburg.replanning.modules;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.hamburg.replanning.modules.PermissibleModesCalculatorFromObjectAttributes;

/**
 * @author zmeng
 */
public class PermissibleModesCalculatorFromObjectAttributesTest {
    private final Person person = PopulationUtils.getFactory().createPerson(Id.create(1L, Person.class));

    @Test
    public void test0(){
        Plan plan = PopulationUtils.createPlan();
        this.person.addPlan(plan);
        PersonUtils.setCarAvail(person, "never");
        String[] availableModes = new String[]{TransportMode.car, TransportMode.bike, TransportMode.pt};
        PermissibleModesCalculatorFromObjectAttributes permissibleModesCalculatorFromObjectAttributes = new PermissibleModesCalculatorFromObjectAttributes(availableModes, true);

        person.getAttributes().putAttribute("bannedModes","pt");

        var permissibleModes = permissibleModesCalculatorFromObjectAttributes.getPermissibleModes(plan);

        boolean result = permissibleModes.contains(TransportMode.bike) && !permissibleModes.contains(TransportMode.car) && !permissibleModes.contains(TransportMode.pt);
        Assert.assertTrue("this person should only not be able to use modes" + permissibleModes.toString() + " than bike", result);
    }

    @Test
    public void test1(){
        Plan plan = PopulationUtils.createPlan();
        this.person.addPlan(plan);
        PersonUtils.setCarAvail(person, "never");
        String[] availableModes = new String[]{TransportMode.car, TransportMode.bike, TransportMode.pt};
        PermissibleModesCalculatorFromObjectAttributes permissibleModesCalculatorFromObjectAttributes = new PermissibleModesCalculatorFromObjectAttributes(availableModes, false);

        person.getAttributes().putAttribute("bannedModes","pt");

        var permissibleModes = permissibleModesCalculatorFromObjectAttributes.getPermissibleModes(plan);

        boolean result = permissibleModes.contains(TransportMode.bike) && permissibleModes.contains(TransportMode.car) && !permissibleModes.contains(TransportMode.pt);
        Assert.assertTrue("this person should only not be able to use modes" + permissibleModes.toString() + " than bike and car", result);
    }

    @Test
    public void test2(){
        Plan plan = PopulationUtils.createPlan();
        this.person.addPlan(plan);
        PersonUtils.setCarAvail(person, "never");
        String[] availableModes = new String[]{TransportMode.car, TransportMode.bike, TransportMode.pt};
        PermissibleModesCalculatorFromObjectAttributes permissibleModesCalculatorFromObjectAttributes = new PermissibleModesCalculatorFromObjectAttributes(availableModes, false);

        //person.getAttributes().putAttribute("bannedModes","pt");

        var permissibleModes = permissibleModesCalculatorFromObjectAttributes.getPermissibleModes(plan);

        boolean result = permissibleModes.contains(TransportMode.bike) && permissibleModes.contains(TransportMode.car) && permissibleModes.contains(TransportMode.pt);
        Assert.assertTrue("this person should only not be able to use modes" + permissibleModes.toString() + " than bike, car and pt", result);
    }
}