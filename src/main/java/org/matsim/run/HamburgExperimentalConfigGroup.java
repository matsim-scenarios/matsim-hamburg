package org.matsim.run;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

/**
 * @author zmeng
 */
public class HamburgExperimentalConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "hamburgExperimental";

    private static final String POPULATION_DOWNSAMPLE_FACTOR = "populationDownsampleFactor";
    private static final String USE_PERSON_INCOME_BASED_SCORING = "usePersonIncomeBasedScoring";
    private static final String FREE_SPEED_FACTOR = "freeSpeedFactor";
    private static final String PARK_PRESSURE_LINK_ATTRIBUTE_FILE = "parkPressureLinkAttributeFile";
    private static final String PARK_PRESSURE_SCORE_PARAMS = "parkPressureScoreParams";
    private static final String PARK_PRESSURE_SCORE_CONSTANT = "parkPressureScoreConstant";
    private static final String Increase_Storage_Capacity = "increaseStorageCapacity";
    private static final String Sample_Size = "sampleSize";
    private static final String Filter_Commercial = "filterCommercial";
    private static final String DRT_NETWORK_OPERATION_AREA = "drtNetworkOperationArea";
    private static final String FIXED_DAILY_MOBILITY_BUDGET = "fixedDailyMobilityBudget";
    private static final String CAR_SHARING_SERVICE_INPUT_FILE = "carSharingServiceInputFile";
    private static final String BIKE_SHARING_SERVICE_INPUT_FILE = "bikeSharingServiceInputFile";
    private static final String SUBTOUR_MODE_CHOICE_PROBA_FOR_SINGLE_TRIP_CHANGE = "smcProbaForSingleTripChange";
    private static final String SCORE_PENALTY_FOR_MASS_CONSERVATION_VIOLATION = "scorePenaltyForMassConservationViolation";


    private static final String PARK_PRESSURE_LINK_ATTRIBUTE_FILE_EXP = "set to null if parkPressure attribute is already defined in the network. Otherwise, a csv with 2 columns (link, value) is expected.";

    public HamburgExperimentalConfigGroup() {
        super(GROUP_NAME);
    }


    private double populationDownsampleFactor = 1.0;
    private boolean usePersonIncomeBasedScoring = true;
    private double freeSpeedFactor = 1.;
    private String parkPressureLinkAttributeFile = null;
    private String parkPressureScoreParams = "1.,0.7,0.";
    private double parkPressureScoreConstant = -1.0;
    private boolean increaseStorageCapacity = true;
    private boolean filterCommercial = false;
    private int sampleSize = 10;
    private String drtNetworkOperationArea = null;
    private Double fixedDailyMobilityBudget = null;
    private String carSharingServiceInputFile = null;
    private String bikeSharingServiceInputFile = null;
    private double smcProbaForSingleTripChange = 0; // same default as in {@link SubtourModeChoiceConfigGroup} //TODO delete and make original config group setting available in MATSim
    private double scorePenaltyForMassConservationViolation = -0.0d;


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

    @StringGetter(DRT_NETWORK_OPERATION_AREA)
    public String getDrtNetworkOperationArea() {
        return drtNetworkOperationArea;
    }
    @StringSetter(DRT_NETWORK_OPERATION_AREA)
    public void setDrtNetworkOperationArea(String drtNetworkOperationArea) {      this.drtNetworkOperationArea = drtNetworkOperationArea;  }

    @StringGetter(FIXED_DAILY_MOBILITY_BUDGET)
    public Double getfixedDailyMobilityBudget() {
        return fixedDailyMobilityBudget;
    }
    @StringSetter(FIXED_DAILY_MOBILITY_BUDGET)
    public void setfixedDailyMobilityBudget(Double fixedDailyMobilityBudget) {this.fixedDailyMobilityBudget = fixedDailyMobilityBudget;}

    @StringGetter(CAR_SHARING_SERVICE_INPUT_FILE)
    public String getCarSharingServiceInputFile() { return carSharingServiceInputFile; }
    @StringSetter(CAR_SHARING_SERVICE_INPUT_FILE)
    public void setCarSharingServiceInputFile(String carSharingServiceInputFile) { this.carSharingServiceInputFile = carSharingServiceInputFile; }

    @StringGetter(BIKE_SHARING_SERVICE_INPUT_FILE)
    public String getBikeSharingServiceInputFile() { return bikeSharingServiceInputFile; }
    @StringSetter(BIKE_SHARING_SERVICE_INPUT_FILE)
    public void setBikeSharingServiceInputFile(String bikeSharingServiceInputFile) { this.bikeSharingServiceInputFile = bikeSharingServiceInputFile; }

    @StringGetter(SUBTOUR_MODE_CHOICE_PROBA_FOR_SINGLE_TRIP_CHANGE)
    public double getSubTourModeChoiceProbaForSingleTripChange() {
        return this.smcProbaForSingleTripChange;
    }

    @StringSetter(SUBTOUR_MODE_CHOICE_PROBA_FOR_SINGLE_TRIP_CHANGE)
    public void setSubTourModeChoiceProbaForSingleTripChange(double val) {
        this.smcProbaForSingleTripChange = val;
    }

    @StringGetter(SCORE_PENALTY_FOR_MASS_CONSERVATION_VIOLATION)
    public double getScorePenaltyForMassConservationViolation() {
        return this.scorePenaltyForMassConservationViolation;
    }

    @StringSetter(SCORE_PENALTY_FOR_MASS_CONSERVATION_VIOLATION)
    public void setScorePenaltyForMassConservationViolation(double penaltyPerViolation) {
        this.scorePenaltyForMassConservationViolation = penaltyPerViolation;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(PARK_PRESSURE_LINK_ATTRIBUTE_FILE, PARK_PRESSURE_LINK_ATTRIBUTE_FILE_EXP);
        return map;
    }
}
