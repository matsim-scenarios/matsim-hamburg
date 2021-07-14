/*
package org.matsim.analysis.mobBug;

*/
/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 * *********************************************************************** *//*



import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.gis.ShapeFileReader;
//import org.matsim.run.HamburgFreightMainModeIdentifier;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

*/
/**
 * Calculates at the end of each iteration mode statistics, based on the main mode identifier of a trip chain.
 * For multi-modal trips, this is only as accurate as your main mode identifier.
 * The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author grybczak
 *//*



class ModeShareInEachDistrict {

    private static Map<String, HashMap<String, Double>> modeStatsInEachDistrict = new HashMap<String, HashMap<String, Double>>();
    //private static HamburgFreightMainModeIdentifier mainModeIdentifier;
    private static Map<String, Double> modeCnt = new HashMap<>();
    private static String shapeFile = "C:\\Users\\Gregor\\Documents\\shared-svn\\projects\\realLabHH\\data\\hamburg_shapeFile\\hamburg_metropo\\hamburg_metropo.shp";
    private static String popInputFile = "D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\input\\hamburg-v1.0-1pct.plans.xml.gz";
    static List<String> modes = (Arrays.asList(TransportMode.car, TransportMode.bike, TransportMode.ride, TransportMode.walk, TransportMode.other));

    public static void main(String[] args) {


        Population pop = PopulationUtils.readPopulation(popInputFile);
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

        for (SimpleFeature feature: features) {
            modeStatsInEachDistrict.put((String) feature.getAttribute("name"), null);
        }
        collectModeShareInfo(pop);
    }


    private static void collectModeShareInfo(Population population) {


        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            List<Trip> trips = TripStructureUtils.getTrips(plan);
            for (Trip trip : trips) {
                //String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                trip.getOriginActivity().getCoord();

                Double cnt = modeCnt.get(mode);
                if (cnt == null) {
                    cnt = 0.;
                }
                modeCnt.put(mode, cnt + 1);
            }
        }

        double sum = 0;
        for (Double val : modeCnt.values()) {
            sum += val;
        }


        //System.out.println("Mode shares over all " + sum + " trips found. MainModeIdentifier: " + mainModeIdentifier.getClass());
        for (String mode : modes) {
            Double cnt = modeCnt.getOrDefault(mode, 0.0);
            double share = 0.;
            if (cnt != null) {
                share = cnt / sum;
            }
            System.out.println("-- mode share of mode " + mode + " = " + share);

        }


    }
}*/
