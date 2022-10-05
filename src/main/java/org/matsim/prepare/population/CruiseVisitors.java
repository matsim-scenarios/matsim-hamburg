package org.matsim.prepare.population;

import org.checkerframework.checker.units.qual.C;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;

import org.matsim.core.population.PopulationUtils;

public class CruiseVisitors {

	public static void main(String[] args) {

		Population population = PopulationUtils.readPopulation("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-1pct-base.plans.xml.gz");

		for(int i = 0; i < 3000; i++) {

			Person person = createPerson("cruise_ship" + i, population.getFactory());
			population.addPerson(person);
		}

		 PopulationUtils.writePopulation(population, "./scenarios/input/cruise-ship-population.xml.gz");

	}

	private static Person createPerson(String id, PopulationFactory factory) {

		Person person = factory.createPerson(Id.createPersonId(id));
		PopulationUtils.putSubpopulation(person, "person");
		PopulationUtils.putPersonAttribute(person, "income", 2000.);
		Plan plan = createPlan(factory);
		person.addPlan(plan);
		return person;
	}

	private static Plan createPlan(PopulationFactory factory) {

		Plan plan = factory.createPlan();

		Coord shipCoord = new Coord(562152.56,5933129.0);
		Activity shipLeaveActivity = factory.createActivityFromCoord("ship", shipCoord);
		shipLeaveActivity.setEndTime(10 * 3600);
		plan.addActivity(shipLeaveActivity);

		Leg shipToCityLeg = factory.createLeg("car");
		plan.addLeg(shipToCityLeg);

		Coord sightseeingCoord = new Coord(565911.5,5934099.0);
		Activity sightSeeingActivity = factory.createActivityFromCoord("sightseeing", sightseeingCoord);
		sightSeeingActivity.setEndTime(13 * 3600);
		plan.addActivity(sightSeeingActivity);

		Leg cityToShip = factory.createLeg("pt");
		plan.addLeg(cityToShip);

		Activity shipEndActivity = factory.createActivityFromCoord("ship", shipCoord);
		shipEndActivity.setStartTime(16 * 3600);
		plan.addActivity(shipEndActivity);

		return plan;
	}
}