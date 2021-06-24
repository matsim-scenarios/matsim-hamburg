package org.matsim.analysis;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author ikaddoura
 */

class AnalysisConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "emissionsAnalysis" ;

    public AnalysisConfigGroup() {
        super(GROUP_NAME);
    }

    private String runDirectory = "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
    private String runId = "berlin-v5.4-1pct";
    private double electricVehicleShare = 0.01;

    @StringGetter( "runDirectory" )
    public String getRunDirectory() {
        return runDirectory;
    }
    @StringSetter( "runDirectory" )
    public void setRunDirectory(String runDirectory) {
        this.runDirectory = runDirectory;
    }

    @StringGetter( "runId" )
    public String getRunId() {
        return runId;
    }

    @StringSetter( "runId" )
    public void setRunId(String runId) {
        this.runId = runId;
    }

    @StringGetter( "vehicleShareChangeToElectricVehicle" )
    public double getElectricVehicleShare() {
        return electricVehicleShare;
    }
    @StringSetter( "vehicleShareChangeToElectricVehicle" )
    public void setElectricVehicleShare(double electricVehicleShare) {
        this.electricVehicleShare = electricVehicleShare;
    }

}

