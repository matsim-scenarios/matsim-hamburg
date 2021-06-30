package org.matsim.sharingFare;

import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.PositiveOrZero;

/**
 * @author zmeng
 */
public class SharingServiceFaresConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_MODE = "mode";
    public static final String BASEFARE = "basefare";
    public static final String TIMEFARE = "timeFare_m";
    public static final String DISTANCEFARE = "distanceFare_m";
    public static final String ID = "id";

    @PositiveOrZero
    private double basefare;
    @PositiveOrZero
    private double timeFare_m;
    @PositiveOrZero
    private double distanceFare_m;

    private String id;


    public SharingServiceFaresConfigGroup() {
        super(GROUP_MODE);
    }

    @StringGetter("id")
    public String getId() {
        return this.id;
    }

    @StringSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @StringGetter("basefare")
    public double getBasefare() {
        return this.basefare;
    }

    @StringSetter("basefare")
    public void setBasefare(double basefare) {
        this.basefare = basefare;
    }

    @StringGetter("timeFare_m")
    public double getTimeFare_m() {
        return this.timeFare_m;
    }

    @StringSetter("timeFare_m")
    public void setTimeFare_m(double timeFare_m) {
        this.timeFare_m = timeFare_m;
    }

    @StringGetter("distanceFare_m")
    public double getDistanceFare_m() {
        return this.distanceFare_m;
    }

    @StringSetter("distanceFare_m")
    public void setDistanceFare_m(double distanceFare_m) {
        this.distanceFare_m = distanceFare_m;
    }
}
