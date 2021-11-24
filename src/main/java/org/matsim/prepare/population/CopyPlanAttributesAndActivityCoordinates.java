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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CopyPlanAttributesAndActivityCoordinates {

	private static final Logger log = Logger.getLogger(CopyPlanAttributesAndActivityCoordinates.class);

	public static void main(String[] args) {

		String idMappingFile = "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v3/20211118_open_hamburg_delivery_senozon/idMapping.csv";
		String fromAttributesFile = "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v3/20211118_open_hamburg_delivery_senozon/personAttributes.xml.gz";
		String fromPlansFile = "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v3/20211118_open_hamburg_delivery_senozon/population.xml.gz";
		String targetPopulationFile = "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.0/input/hamburg-v2.0-25pct.plans.xml.gz.gz";
		String outputFile = "D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v2/hamburg-v2.0/input/hamburg-v3.0-25pct.plans-firstVersion-freight-as-closed.xml.gz";

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

		Population fromPopulation = loadFromPlans(fromAttributesFile, fromPlansFile);
		Population toPopulation = PopulationUtils.readPopulation(targetPopulationFile);


		log.info("START DELETING OLD ATTRIBUTES");
		log.info("######################################################################");

		for (Person toPerson : toPopulation.getPersons().values()) {
			//remove attributes
			PopulationUtils.removePersonAttribute(toPerson, "IPD_actEndTimes");
			PopulationUtils.removePersonAttribute(toPerson, "IPD_actStartTimes");
			PopulationUtils.removePersonAttribute(toPerson, "IPD_actTypes");
			PopulationUtils.removePersonAttribute(toPerson, "sim_regionType");
			PopulationUtils.removePersonAttribute(toPerson, "marginalUtilityOfMoney");
			PopulationUtils.removePersonAttribute(toPerson, "income"); //we will override the income attribute
		}

		log.info("START COPYING ATTRIBUTES");
		log.info("######################################################################");

		long numberOfPersonsToHandle = toPopulation.getPersons().values().stream()
				.filter(person -> PopulationUtils.getSubpopulation(person).equals("person"))
				.count();
		int handledPersons = 0;

		for (Person fromPerson : fromPopulation.getPersons().values()) {
			Person toPerson = toPopulation.getPersons().get(idMap.get(fromPerson.getId()));

			if(toPerson == null){
				throw new IllegalArgumentException("could not find person " + idMap.get(fromPerson.getId()) + " in target population. Was mapped to " + fromPerson.getId() + " in fromPopulation");
			}


			//check socio-demographic attributes
			if(PersonUtils.getAge(fromPerson) != PersonUtils.getAge(toPerson)){
				log.warn("age attribute does not match for " + fromPerson.getId() + " and " + toPerson.getId());
			}
			if(! PopulationUtils.getPersonAttribute(fromPerson, "gender").equals(PopulationUtils.getPersonAttribute(toPerson, "gender"))){
				log.warn("gender attribute does not match for " + fromPerson.getId() + " and " + toPerson.getId());
			}

			//copy Attributes
			String incomeGroupString = (String) PopulationUtils.getPersonAttribute(fromPerson, "householdincome");
			String householdSizeString = (String) PopulationUtils.getPersonAttribute(fromPerson, "householdsize");
			PopulationUtils.putPersonAttribute(toPerson, "householdincome", incomeGroupString);
			PopulationUtils.putPersonAttribute(toPerson, "householdsize", householdSizeString);

			//compute and set new income
			final Random rnd = new Random(1234);
			double income = drawIncome(incomeGroupString, householdSizeString, rnd);
			PersonUtils.setIncome(toPerson, income);

			List<Activity> fromActivities = TripStructureUtils.getActivities(fromPerson.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			List<Activity> toActivities = TripStructureUtils.getActivities(toPerson.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);


			//in the toPopulation, we inserted short trips according to the following pattern:
			// ... originaTrip -> originalOrigin -> additionalTrip -> otherActivity -> additionalTrip -> originalOrigin -> originalTrip -> originalDestination
			//this means, we have to make sure to copy the coordinates to the corresponding activity only! And to also copy it to the second occurrence of the originalOrigin!
			int toActivityCounter = 0;
			for (int fromActivityCounter = 0; fromActivityCounter < fromActivities.size(); fromActivityCounter++) {
				Activity fromActivity = fromActivities.get(fromActivityCounter);
				Activity toActivity = toActivities.get(toActivityCounter);

				if(toActivity.getType().startsWith("other")){
					Activity secondOccurrenceOfLastFromActivity = toActivities.get(toActivityCounter + 1);
					copyCoord(fromPerson.getId(), fromActivities.get(fromActivityCounter - 1), secondOccurrenceOfLastFromActivity);

					toActivityCounter += 2;
					toActivity = toActivities.get(toActivityCounter);
				}
				copyCoord(fromPerson.getId(), fromActivity, toActivity);
				toActivityCounter ++;
			}
			handledPersons ++;
		}

		if(numberOfPersonsToHandle != handledPersons){
			throw new IllegalStateException("number of persons in target population = " + numberOfPersonsToHandle + ". Handled persons = " + handledPersons + ". Should be equal!");
		}

		log.info("START DUMPING OUTPUT");
		log.info("######################################################################");
		PopulationUtils.writePopulation(toPopulation, outputFile);
		log.info("FINISHED");
		log.info("######################################################################");
	}

	private static Population loadFromPlans(String fromAttributesFile, String fromPlansFile) {
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

	private static void copyCoord(Id<Person> fromPersonId, Activity fromActivity, Activity toActivity) {
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
