package org.matsim.prepare.population;

import org.checkerframework.checker.units.qual.C;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.Random;

public class AddCruiseShip {

	private static final Coord start = new Coord(0,0);
	private static final Random random = new Random();

	public static void main(String[] args) {

		var population = PopulationUtils.readPopulation("");
		var geometry = ShapeFileReader.getAllFeatures("").stream()
				.map(feature -> (Geometry) feature.getDefaultGeometry())
				.limit(1)
				.findAny()
				.orElseThrow();

		for (var i = 0; i < 1000; i++ ) {
			var person = createPerson("cruise_guest_" + i, geometry, population.getFactory());
		}

	}

	private static Person createPerson(String id, Geometry geometry, PopulationFactory factory) {

		var person = factory.createPerson(Id.createPersonId(id));
		var plan = createPlan(geometry, factory);
		person.addPlan(plan);
		return person;
	}

	private static Plan createPlan(Geometry geometry, PopulationFactory factory) {

		var plan = factory.createPlan();

		var startAct = factory.createActivityFromCoord("ship", start);
		startAct.setEndTime(9*3600);
		plan.addActivity(startAct);

		var startToCityLeg = factory.createLeg("pt");
		plan.addLeg(startToCityLeg);

		var randomCoord = createRandomCoord(geometry);
		var cityAct = factory.createActivityFromCoord("city", randomCoord);
		cityAct.setEndTime(13*3600);
		plan.addActivity(cityAct);

		var cityToEndLeg = factory.createLeg("pt");
		plan.addLeg(cityToEndLeg);

		var endAct = factory.createActivityFromCoord("ship", start);
		endAct.setStartTime(13.5 * 3600);
		plan.addActivity(endAct);

		return plan;
	}

	private static Coord createRandomCoord(Geometry geometry) {

		var envelope = geometry.getEnvelopeInternal();
		double x, y;
		Point point;

		do {
			x = envelope.getMinX() + envelope.getWidth() * random.nextDouble();
			y = envelope.getMinY() + envelope.getHeight() * random.nextDouble();
			point = MGC.xy2Point(x, y);
		} while (point == null || geometry.contains(point));

		return new Coord(x, y);
	}
}