package org.matsim.run;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author zmeng
 */
public class HereAPITravelTimeValidationConfigGroup extends ReflectiveConfigGroup {

    private static final String GROUP_NAME = "HereAPITravelTimeValidation";

    private static final String USE_HERE_API = "useHereAPI";
    private static final String HERE_MAPS_API_KEY = "HereMapsAPIKey";
    private static final String DATE = "date";
    private static final String NUM_OF_TRIPS = "numOfTrips";

    private boolean useHereAPI = false;
    private String HereMapsAPIKey = null;
    private String date = null;
    private String numOfTrips = "all";

    public HereAPITravelTimeValidationConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter(USE_HERE_API)
    public boolean isUseHereAPI() {
        return useHereAPI;
    }
    @StringSetter(USE_HERE_API)
    public void setUseHereAPI(boolean useHereAPI) {
        this.useHereAPI = useHereAPI;
    }
    @StringGetter(HERE_MAPS_API_KEY)
    public String getHereMapsAPIKey() {
        return HereMapsAPIKey;
    }
    @StringSetter(HERE_MAPS_API_KEY)
    public void setHereMapsAPIKey(String hereMapsAPIKey) {
        HereMapsAPIKey = hereMapsAPIKey;
    }
    @StringGetter(DATE)
    public String getDate() {
        return date;
    }
    @StringSetter(DATE)
    public void setDate(String date) {
        this.date = date;
    }
    @StringGetter(NUM_OF_TRIPS)
    public String getNumOfTrips() {
        return numOfTrips;
    }
    @StringSetter(NUM_OF_TRIPS)
    public void setNumOfTrips(String numOfTrips) {
        this.numOfTrips = numOfTrips;
    }
}