package org.matsim.hamburg.replanning.modules;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.TripStructureUtils;

/**
 * @author zmeng
 */
public class HamburgSubtourModeChoice extends AbstractMultithreadedModule {

    private PermissibleModesCalculator permissibleModesCalculator;

    private final String[] chainBasedModes;
    private final String[] modes;

    private SubtourModeChoice.Behavior behaviour;

    private double probaForRandomSingleTripMode;

    public HamburgSubtourModeChoice(final Config config,
                                    PermissibleModesCalculator permissableModes) {
        super(config.global().getNumberOfThreads());
        this.modes = config.subtourModeChoice().getModes().clone();
        this.behaviour = config.subtourModeChoice().getBehavior();
        this.probaForRandomSingleTripMode = config.subtourModeChoice().getProbaForRandomSingleTripMode();
        this.chainBasedModes = config.subtourModeChoice().getChainBasedModes().clone();
        this.permissibleModesCalculator = permissableModes;
    }

    public HamburgSubtourModeChoice(final Config config) {
        this(config, new PermissibleModesCalculatorFromObjectAttributes(
                config.subtourModeChoice().getModes().clone(), config.subtourModeChoice().considerCarAvailability()));
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        final ChooseRandomLegModeForSubtour chooseRandomLegMode = new ChooseRandomLegModeForSubtour(
                TripStructureUtils.getRoutingModeIdentifier(), this.permissibleModesCalculator, this.modes,
                this.chainBasedModes, MatsimRandom.getLocalInstance(), behaviour, probaForRandomSingleTripMode);
        return chooseRandomLegMode;
    }

}
