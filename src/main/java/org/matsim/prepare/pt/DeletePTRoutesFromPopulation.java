/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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
 * *********************************************************************** */

package org.matsim.prepare.pt;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.analysis.HamburgIntermodalAnalysisModeIdentifier;

public class DeletePTRoutesFromPopulation {

	public static void main(String[] args) {
		Population population = PopulationUtils.readPopulation("provide input plans");
		deletePTRoutesFromPopulation(population);
		PopulationUtils.writePopulation(population, "provide output path");
	}

	public static void deletePTRoutesFromPopulation(Population population){

		HamburgIntermodalAnalysisModeIdentifier mainModeIdentifier = new HamburgIntermodalAnalysisModeIdentifier();

		population.getPersons().values().stream()
				.flatMap(person -> person.getPlans().stream())
				.flatMap(plan -> TripStructureUtils.getTrips(plan).stream())
				.filter(trip -> mainModeIdentifier.identifyMainMode(trip.getTripElements()).contains("pt"))
				.flatMap(trip -> trip.getLegsOnly().stream())
				.forEach(leg -> leg.setRoute(null));
	}





}
