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
    private static final String PARK_PRESSURE_SCORE_PARAMS = "parkPressureScoreParams";
    private static final String PARK_PRESSURE_SCORE_CONSTANT = "parkPressureScoreConstant";
    private static final String Increase_Storage_Capacity = "increaseStorageCapacity";
    private static final String Sample_Size = "sampleSize";
    private static final String Filter_Commercial = "filterCommercial";
    private static final String DRT_OPERATION_AREA = "drtOperationArea";
    private static final String E_SCOOTER_SERVICE_AREA = "eScooterServiceArea";

    public HamburgExperimentalConfigGroup() {
        super(GROUP_NAME);
    }

    private double populationDownsampleFactor = 1.0;
    private boolean usePersonIncomeBasedScoring = false;
    private double freeSpeedFactor = 1.;
    private boolean useLinkBasedParkPressure = false;
    private String parkPressureLinkAttributeFile = null;
    private String parkPressureScoreParams = "1.,0.7,0.";
    private double parkPressureScoreConstant = 0.;
    private boolean increaseStorageCapacity = true;
    private boolean filterCommercial = false;
    private int sampleSize = 10;
    private String drtOperationArea = null;
    private String eScooterServiceArea = null;

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
    @StringGetter(PARK_PRESSURE_SCORE_PARAMS)
    public String getParkPressureScoreParams() {
        return parkPressureScoreParams;
    }
    @StringSetter(PARK_PRESSURE_SCORE_PARAMS)
    public void setParkPressureScoreParams(String parkPressureScoreParams) {
        this.parkPressureScoreParams = parkPressureScoreParams;
    }
    @StringGetter(PARK_PRESSURE_SCORE_CONSTANT)
    public double getParkPressureScoreConstant() {
        return parkPressureScoreConstant;
    }
    @StringSetter(PARK_PRESSURE_SCORE_CONSTANT)
    public void setParkPressureScoreConstant(double parkPressureScoreConstant) {
        this.parkPressureScoreConstant = parkPressureScoreConstant;
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

    @StringGetter(DRT_OPERATION_AREA)
    public String getDrtOperationArea() {
        return drtOperationArea;
    }
    @StringSetter(DRT_OPERATION_AREA)
    public void setDrtOperationArea(String drtOperationArea) {      this.drtOperationArea = drtOperationArea;  }

    @StringGetter(E_SCOOTER_SERVICE_AREA)
    public String getEScooterServiceArea() {
        return eScooterServiceArea;
    }
    @StringSetter(E_SCOOTER_SERVICE_AREA)
    public void setEScooterServiceArea(String eScooterServiceArea) {
        this.eScooterServiceArea = eScooterServiceArea;
    }
}
