package org.matsim.hamburg.replanning.modules;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zmeng
 */
public class PermissibleModesCalculatorFromObjectAttributes implements PermissibleModesCalculator {
    public static final String BANNED_MODES = "bannedModes";

    private final List<String> availableModes;
    private final boolean considerCarAvailability;

    public PermissibleModesCalculatorFromObjectAttributes(final String[] availableModes, final boolean considerCarAvailability) {
        this.availableModes = Arrays.asList(availableModes);
        this.considerCarAvailability = considerCarAvailability;
    }

    @Override
    public Collection<String> getPermissibleModes(final Plan plan) {
        Collection<String> bannedModes = new HashSet<>();
        boolean carAvail = !"no".equals(PersonUtils.getLicense(plan.getPerson())) && !"never".equals(PersonUtils.getCarAvail(plan.getPerson()));
        if(considerCarAvailability && !carAvail)
            bannedModes.add(TransportMode.car);

        final Object o = plan.getPerson().getAttributes().getAttribute(BANNED_MODES);
        if(o != null){
            try {
                String[] baModes = o.toString().split(",");
                bannedModes.addAll(Arrays.asList(baModes.clone()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid value for enum CarAvailability: " + o.toString() + ". Aborting!");
            }
        }
        return this.availableModes.stream().filter(mode -> !bannedModes.contains(mode)).collect(Collectors.toList());
    }
}