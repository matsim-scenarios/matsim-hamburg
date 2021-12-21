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

package org.matsim.prepare.population;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.prepare.freight.bvm.CreatFreightAgents;

public class MergeFreightPlans {

	public static void main(String[] args) {

		Population withoutFreight = PopulationUtils.readPopulation("D:/ReallabHH/v3.0/25pct/hv3-25-2/hamburg-v2.2-25pct-base.output_plans.xml.gz");
		Population withFreight = PopulationUtils.readPopulation("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.0/input/hamburg-v2.0-25pct.plans.xml.gz");

		withFreight.getPersons().values().stream()
				.filter(person -> person.getId().toString().startsWith(CreatFreightAgents.COMMERCIAL))
				.forEach(person -> withoutFreight.addPerson(person));

		PopulationUtils.writePopulation(withoutFreight, "D:/ReallabHH/v3.0/25pct/hv3-25-2/hamburg-v3.0-25pct.plans-not-calibrated-wFreight.xml.gz");

	}
}
