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
    private static final String Increase_Storage_Capacity = "increaseStorageCapacity";
    private static final String Sample_Size = "sampleSize";
    private static final String Filter_Commercial = "filterCommercial";

    public HamburgExperimentalConfigGroup() {
        super(GROUP_NAME);
    }

    private double populationDownsampleFactor = 1.0;
    private boolean usePersonIncomeBasedScoring = false;
    private double freeSpeedFactor = 1.;
    private boolean useLinkBasedParkPressure = false;
    private String parkPressureLinkAttributeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.1/hamburg-v1.1-link2parkpressure.csv";
    private String parkPressureBasedParkTime = "1200.,720.,0.";
    private boolean increaseStorageCapacity = true;
    private boolean filterCommercial = false;
    private int sampleSize = 10;

    @StringGetter(Filter_Commercial)
    public boolean isFilterCommercial() {
        return filterCommercial;
    }
    @StringSetter(Filter_Commercial)
    public void setFilterCommercial(boolean filterCommercial) {
        this.filterCommercial = filterCommercial;
    }
    @StringGetter(Sample_Size)
    public int getSampleSize() {
        return sampleSize;
    }
    @StringSetter(Sample_Size)
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }
    @StringGetter(Increase_Storage_Capacity)
    public boolean isIncreaseStorageCapacity() {
        return increaseStorageCapacity;
    }

    @StringSetter(Increase_Storage_Capacity)
    public void setIncreaseStorageCapacity(boolean increaseStorageCapacity) {
        this.increaseStorageCapacity = increaseStorageCapacity;
    }

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
