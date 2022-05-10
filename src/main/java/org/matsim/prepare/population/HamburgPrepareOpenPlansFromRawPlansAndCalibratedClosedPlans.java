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

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.prepare.population.ResolveGridCoordinates;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBaseCaseHamburgScenario;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * this class merges information from two sources.
 * <ul>
 *     <li>It takes (person) agent id's and attributes from the raw population file provided by senozon (and computes a resulting personal income attribute)
 *     <li>It takes the activity chains (and leg modes) from closed, calibrated plans that were used inside the ReallabHH project.
 *     		These plans contain additional short trips from and to "other" activities that had been inserted to better match the trip distance distribution and number of trips.
 *     <li>It modifies the activity coordinates such they fit the open raw data (from where the attributes are taken) - which are on a 300m grid
 *     <li>output population is dumped out.
 *     <li> finally, activity coordinates are re-distributed within the grid using {@link org.matsim.application.prepare.population.ResolveGridCoordinates} in matsim-application contrib. output is overridden.
 *     	for the mapping of activities to land use geometries, no specific filter or matching is conducted. //TODO this leaves room for improvement. for example, home acts could be exclusively matched to residential geoms etc.
 *     <li> merged with freight plans. //TODO
 * </ul>
 */
public class HamburgPrepareOpenPlansFromRawPlansAndCalibratedClosedPlans {

	private static final Logger log = Logger.getLogger(HamburgPrepareOpenPlansFromRawPlansAndCalibratedClosedPlans.class);

	public static void main(String[] args) {

		String idMappingFile = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v3/20211118_open_hamburg_delivery_senozon/idMapping.csv";
		String attributesFile = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v3/20211118_open_hamburg_delivery_senozon/personAttributes.xml.gz";
		String plansWithCoordinatesAndIds = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v3/20211118_open_hamburg_delivery_senozon/population.xml.gz";
		String plansWithAllTrips = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.0/input/hamburg-v2.0-25pct.plans.xml.gz";

		//CAUTION: DO NOT USE A NETWORK THAT HAS NON CAR LINKS! WILL LEAD TO PROBLEMS IN THE QSIM IN POLICY CASES AS ACTIVITIES WILL BE ATTACHED TO NON CAR-LINK
		String targetNetwork = "";
		String crs = RunBaseCaseHamburgScenario.COORDINATE_SYSTEM;
		String outputFile = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v3/hamburg-v3.0-25pct.plans-not-calibrated.xml.gz";
		String landUseShapeFile = "../../svn/shared-svn/projects/german-wide-freight/landuse/landuse.shp";

		final Random rnd = new Random(1234);

		Map<Id<Person>, Id<Person>> idMap = new HashMap<>();
		log.info("start to read idMapping File");
		try {
			CSVReader reader = new CSVReader(Files.newBufferedReader(Paths.get(idMappingFile)));
			String line[];
			while ((line = reader.readNext()) != null) {
				idMap.put(Id.createPersonId(line[0]), Id.createPersonId(line[1])); //csv has old id in second column and new id in first column. we copy coordinates from new to old.
			}
		} catch (IOException | CsvValidationException e) {
			e.printStackTrace();
		}
		log.info("finished to read idMapping File");

//		Network network = NetworkUtils.readTimeInvariantNetwork(targetNetwork);
		Network network = null; //we resolve the grid coordinates later and map them to the corresponding links afterwards so we can save reading the network here. If no grid resolving is conducted, uncomment the line above.

		Population attributesAndCoordinatesPopulation = loadFromPlansWithExternalAttributesFile(attributesFile, plansWithCoordinatesAndIds);
		Population plansPopulation = PopulationUtils.readPopulation(plansWithAllTrips);
		Population outputPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = outputPopulation.getFactory();

		log.info("START COPYING ATTRIBUTES AND COORDINATES");
		log.info("######################################################################");

		long numberOfPersonsToHandle = plansPopulation.getPersons().values().stream()
				.filter(person -> PopulationUtils.getSubpopulation(person).equals("person"))
				.count();
		int handledPersons = 0;

		for (Person attributedPerson : attributesAndCoordinatesPopulation.getPersons().values()) {
			Person personWithPlan = plansPopulation.getPersons().get(idMap.get(attributedPerson.getId()));
			if(personWithPlan == null){
				throw new IllegalArgumentException("could not find person " + idMap.get(attributedPerson.getId()) + " in population with target plans. Was mapped to " + attributedPerson.getId() + " in attributesAndCoordinatesPopulation");
			}

			//check socio-demographic attributes
			if(PersonUtils.getAge(attributedPerson) != PersonUtils.getAge(personWithPlan)){
				log.warn("age attribute does not match for " + attributedPerson.getId() + " and " + personWithPlan.getId());
			}
			if(! PopulationUtils.getPersonAttribute(attributedPerson, "gender").equals(PopulationUtils.getPersonAttribute(personWithPlan, "gender"))){
				log.warn("gender attribute does not match for " + attributedPerson.getId() + " and " + personWithPlan.getId());
			}
			if(! PopulationUtils.getSubpopulation(personWithPlan).equals("person")){
				throw new IllegalStateException("not treating a person!? agentId=" + personWithPlan.getId());
			}

			//we need to change the id which is not allowed while in a population so we need to remove and re-add
			Person targetPerson = factory.createPerson(attributedPerson.getId());
			targetPerson.addPlan(personWithPlan.getSelectedPlan());
			outputPopulation.addPerson(targetPerson);
			PopulationUtils.putSubpopulation(targetPerson, "person");

			//copy Attributes
			String incomeGroupString = (String) PopulationUtils.getPersonAttribute(attributedPerson, "householdincome");
			String householdSizeString = (String) PopulationUtils.getPersonAttribute(attributedPerson, "householdsize");
			PopulationUtils.putPersonAttribute(targetPerson, "householdincome", incomeGroupString);
			PopulationUtils.putPersonAttribute(targetPerson, "householdsize", householdSizeString);

			//compute and set new income
			double income = drawIncome(incomeGroupString, householdSizeString, rnd);
			PersonUtils.setIncome(targetPerson, income);

			List<Activity> activitiesWithRightCoordinates = TripStructureUtils.getActivities(attributedPerson.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			List<Activity> activitiesToBeOverridden = TripStructureUtils.getActivities(targetPerson.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			//in the plansPopulation, we inserted short trips according to the following pattern:
			// ... originaTrip -> originalOrigin -> additionalTrip -> otherActivity -> additionalTrip -> originalOrigin -> originalTrip -> originalDestination
			//this means, we have to make sure to copy the coordinates to the corresponding activity only! And to also copy it to the second occurrence of the originalOrigin!
			int toActivityCounter = 0;
			for (int fromActivityCounter = 0; fromActivityCounter < activitiesWithRightCoordinates.size(); fromActivityCounter++) {
				Activity fromActivity = activitiesWithRightCoordinates.get(fromActivityCounter);
				Activity toActivity = activitiesToBeOverridden.get(toActivityCounter);

				if(toActivity.getType().startsWith("other")){
					Activity secondOccurrenceOfLastFromActivity = activitiesToBeOverridden.get(toActivityCounter + 1);
					copyCoordAndAdjustLinkId(attributedPerson.getId(), activitiesWithRightCoordinates.get(fromActivityCounter - 1), secondOccurrenceOfLastFromActivity, network);

					toActivityCounter += 2;
					toActivity = activitiesToBeOverridden.get(toActivityCounter);
				}
				copyCoordAndAdjustLinkId(attributedPerson.getId(), fromActivity, toActivity, network);
				toActivityCounter ++;
			}
			
			//remove routes
			for (Leg leg : TripStructureUtils.getLegs(targetPerson.getSelectedPlan())) {
				leg.setRoute(null);
			}
			
			handledPersons ++;
		}

		if(numberOfPersonsToHandle != handledPersons){
			throw new IllegalStateException("number of persons in target population = " + numberOfPersonsToHandle + ". Handled persons = " + handledPersons + ". Should be equal!");
		}
		log.info("######################################################################");


		log.info("START DUMPING OUTPUT WITH GRID COORDINATES");
		log.info("######################################################################");
		PopulationUtils.writePopulation(outputPopulation, outputFile);

		log.info("START RESOLVING GRID COORDINATES");
		log.info("######################################################################");

		args = new String[]{
				outputFile,
				"--input-crs=" + crs,
				"--grid-resolution=300",
				"--landuse=" + landUseShapeFile,
				"--network=" + targetNetwork,
				"--output=" + outputFile //override
		};
		System.exit(new CommandLine(new ResolveGridCoordinates()).execute(args));

		log.info("FINISHED");
//		log.info("######################################################################");
	}

	private static Population loadFromPlansWithExternalAttributesFile(String fromAttributesFile, String fromPlansFile) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputPersonAttributeFile(fromAttributesFile);
		config.plans().setInputFile(fromPlansFile);
		config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
		config.global().setCoordinateSystem("EPSG:25832");
		config.plans().setInputCRS("EPSG:25832");

		//population where to copy the activity coordinates and attributes from
		Population fromPopulation = ScenarioUtils.loadScenario(config).getPopulation();
		return fromPopulation;
	}

	private static void copyCoordAndAdjustLinkId(Id<Person> fromPersonId, Activity fromActivity, Activity toActivity, Network network) {
		if (Math.abs(fromActivity.getCoord().getX() - toActivity.getCoord().getX()) > 300){
			log.warn("x coordinate of fromActivity " + fromActivity + " is more than 300 meters away from x coordinate of toActivity. Check fromPerson " + fromPersonId);
		}
		if (Math.abs(fromActivity.getCoord().getY() - toActivity.getCoord().getY()) > 300){
			log.warn("y coordinate of fromActivity " + fromActivity + " is more than 300 meters away from y coordinate of toActivity. Check fromPerson " + fromPersonId);
		}
		if(! toActivity.getType().startsWith(fromActivity.getType())){
			throw new IllegalArgumentException("attempting to copy coordinates for non-matching activity types! fromActivity=" + fromActivity.getType() + " toActivity=" + toActivity.getType());
		}
		toActivity.setCoord(fromActivity.getCoord());
		if (network != null) {
			Link link = NetworkUtils.getNearestLink(network, toActivity.getCoord());
			if (link != null)
				toActivity.setLinkId(link.getId());
		}
	}

	private static double drawIncome(String incomeGroupString, String householdSizeString, Random rnd) {
		int incomeGroup = 0;
		double householdSize = 1;
		if (incomeGroupString != null && householdSizeString != null) {
			incomeGroup = Integer.parseInt(incomeGroupString);
			householdSize = Double.parseDouble(householdSizeString);
		}

		double income = 0;
		switch (incomeGroup) {
			case 1:
				income = 500 / householdSize;
				break;
			case 2:
				income = (rnd.nextInt(400) + 500) / householdSize;
				break;
			case 3:
				income = (rnd.nextInt(600) + 900) / householdSize;
				break;
			case 4:
				income = (rnd.nextInt(500) + 1500) / householdSize;
				break;
			case 5:
				income = (rnd.nextInt(1000) + 2000) / householdSize;
				break;
			case 6:
				income = (rnd.nextInt(1000) + 3000) / householdSize;
				break;
			case 7:
				income = (rnd.nextInt(1000) + 4000) / householdSize;
				break;
			case 8:
				income = (rnd.nextInt(1000) + 5000) / householdSize;
				break;
			case 9:
				income = (rnd.nextInt(1000) + 6000) / householdSize;
				break;
			case 10:
				income = (Math.abs(rnd.nextGaussian()) * 1000 + 7000) / householdSize;
				break;
			default:
				income = 2364; // Average monthly household income per Capita (2021). See comments below for details
				break;
			// Average Gross household income: 4734 Euro
			// Average household size: 83.1M persons /41.5M households = 2.0 persons / household
			// Average household income per capita: 4734/2.0 = 2364 Euro
			// Source (Access date: 21 Sep. 2021):
			// https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
			// https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
			// https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html;jsessionid=E0D7A060D654B31C3045AAB1E884CA75.live711

		}
		if(income == 0) throw new IllegalStateException();
		return income;
	}


}
