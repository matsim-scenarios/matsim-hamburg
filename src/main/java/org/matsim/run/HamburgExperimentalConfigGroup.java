package org.matsim.run;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author zmeng
 */
public class HamburgExperimentalConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "hamburgExperimental";

    private static final String POPULATION_DOWNSAMPLE_FACTOR = "populationDownsampleFactor";
    private static final String USE_PERSON_INCOME_BASED_SCORING = "usePersonIncomeBasedScoring";
    private static final String FREE_SPEED_FACTOR = "freeSpeedFactor";

    public HamburgExperimentalConfigGroup() {
        super(GROUP_NAME);
    }

    private double populationDownsampleFactor = 1.0;
    private boolean usePersonIncomeBasedScoring = false;
    private double freeSpeedFactor = 1.;

    @StringGetter(POPULATION_DOWNSAMPLE_FACTOR)
    public double getPopulationDownsampleFactor() {
        return populationDownsampleFactor;
    }
    @StringSetter(POPULATION_DOWNSAMPLE_FACTOR)
    public void setPopulationDownsampleFactor(double populationDownsampleFactor) {
        this.populationDownsampleFactor = populationDownsampleFactor;
    }
    @StringGetter(USE_PERSON_INCOME_BASED_SCORING)
    public boolean isUsePersonIncomeBasedScoring() {
        return usePersonIncomeBasedScoring;
    }
    @StringSetter(USE_PERSON_INCOME_BASED_SCORING)
    public void setUsePersonIncomeBasedScoring(boolean usePersonIncomeBasedScoring) {
        this.usePersonIncomeBasedScoring = usePersonIncomeBasedScoring;
    }
    @StringGetter(FREE_SPEED_FACTOR)
    public double getFreeSpeedFactor() {
        return freeSpeedFactor;
    }
    @StringSetter(FREE_SPEED_FACTOR)
    public void setFreeSpeedFactor(double freeSpeedFactor) {
        this.freeSpeedFactor = freeSpeedFactor;
    }
}
