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

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.HamburgAnalysisMainModeIdentifier;
import org.matsim.run.HamburgIntermodalAnalysisModeIdentifier;

public class DeletePTRoutesFromPopulation {

	public static void main(String[] args) {
		Population population = PopulationUtils.readPopulation("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.0/input/hamburg-v2.0-10pct.plans.xml.gz");
		deletePTRoutesFromPopulation(population);
		PopulationUtils.writePopulation(population, "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.0/input/hamburg-v2.0-10pct.plans.wOptRoutes.xml.gz");
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
