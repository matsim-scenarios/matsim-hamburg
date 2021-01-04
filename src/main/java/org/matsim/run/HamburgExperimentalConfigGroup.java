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
    private static final String USE_Link_BASED_PARK_PRESSURE = "useLinkBasedParkPressure";
    private static final String PARK_PRESSURE_LINK_ATTRIBUTE_FILE = "parkPressureLinkAttributeFile";
    private static final String PARK_PRESSURE_BASED_PARK_TIME = "parkPressureBasedParkTime";

    public HamburgExperimentalConfigGroup() {
        super(GROUP_NAME);
    }

    private double populationDownsampleFactor = 1.0;
    private boolean usePersonIncomeBasedScoring = false;
    private double freeSpeedFactor = 1.;
    private boolean useLinkBasedParkPressure = false;
    private String parkPressureLinkAttributeFile = "link2parkpressure.csv";
    private String parkPressureBasedParkTime = "1200.,720.,0.";

    @StringGetter(USE_Link_BASED_PARK_PRESSURE)
    public boolean isUseLinkBasedParkPressure() {
        return useLinkBasedParkPressure;
    }
    @StringSetter(USE_Link_BASED_PARK_PRESSURE)
    public void setUseLinkBasedParkPressure(boolean useLinkBasedParkPressure) {
        this.useLinkBasedParkPressure = useLinkBasedParkPressure;
    }
    @StringGetter(PARK_PRESSURE_LINK_ATTRIBUTE_FILE)
    public String getParkPressureLinkAttributeFile() {
        return parkPressureLinkAttributeFile;
    }
    @StringSetter(PARK_PRESSURE_LINK_ATTRIBUTE_FILE)
    public void setParkPressureLinkAttributeFile(String parkPressureLinkAttributeFile) {
        this.parkPressureLinkAttributeFile = parkPressureLinkAttributeFile;
    }
    @StringGetter(PARK_PRESSURE_BASED_PARK_TIME)
    public String getParkPressureBasedParkTime() {
        return parkPressureBasedParkTime;
    }
    @StringSetter(PARK_PRESSURE_BASED_PARK_TIME)
    public void setParkPressureBasedParkTime(String parkPressureBasedParkTime) {
        this.parkPressureBasedParkTime = parkPressureBasedParkTime;
    }

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
