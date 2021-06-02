package org.matsim.escooter;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author zmeng
 */
public class EScooterConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "eScooter";

    private static final String MODE = "mode";
    private static final String E_SCOOTER_SERVICE_AREA = "eScooterServiceArea";
    private static final String BASE_FARE = "baseFare";
    private static final String FARE_PER_Min = "farePerMin";
    private static final String FARE_PER_KM = "farePerKm";
    private static final String BEELINE_DISTANCE_FACTOR = "beelineDistanceFactor";
    private static final String TELEPORTED_SPEED = "teleportedSpeed";



    public EScooterConfigGroup() {
        super(GROUP_NAME);
    }


    private String eScooterServiceArea = null;
    private String mode = "eScooter";
    private double baseFare = 1.;
    private double farePerMin = 0.2;
    private double farePerKm = 0;
    private double beelineDistanceFactor = 1.3;
    private double teleportedSpeed = 3.1388889;


    @StringGetter(E_SCOOTER_SERVICE_AREA)
    public String getEScooterServiceArea() {
        return eScooterServiceArea;
    }
    @StringSetter(E_SCOOTER_SERVICE_AREA)
    public void setEScooterServiceArea(String eScooterServiceArea) {
        this.eScooterServiceArea = eScooterServiceArea;
    }
    @StringGetter(MODE)
    public String getMode() {
        return mode;
    }
    @StringSetter(MODE)
    public void setMode(String mode) {
        this.mode = mode;
    }
    @StringGetter(BASE_FARE)
    public double getBaseFare() {
        return baseFare;
    }
    @StringSetter(BASE_FARE)
    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }
    @StringGetter(FARE_PER_Min)
    public double getFarePerMin() {
        return farePerMin;
    }
    @StringSetter(FARE_PER_Min)
    public void setFarePerMin(double farePerMin) {
        this.farePerMin = farePerMin;
    }
    @StringGetter(FARE_PER_KM)
    public double getFarePerKm() {
        return farePerKm;
    }
    @StringSetter(FARE_PER_KM)
    public void setFarePerKm(double farePerKm) {
        this.farePerKm = farePerKm;
    }
    @StringGetter(BEELINE_DISTANCE_FACTOR)
    public double getBeelineDistanceFactor() {
        return beelineDistanceFactor;
    }
    @StringSetter(BEELINE_DISTANCE_FACTOR)
    public void setBeelineDistanceFactor(double beelineDistanceFactor) {
        this.beelineDistanceFactor = beelineDistanceFactor;
    }
    @StringGetter(TELEPORTED_SPEED)
    public double getTeleportedSpeed() {
        return teleportedSpeed;
    }
    @StringSetter(TELEPORTED_SPEED)
    public void setTeleportedSpeed(double teleportedSpeed) {
        this.teleportedSpeed = teleportedSpeed;
    }
}
