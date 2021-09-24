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

package org.matsim.massConservation4SingleTripModeChoice;

import com.google.common.base.Preconditions;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.HamburgExperimentalConfigGroup;

import javax.inject.Inject;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


//TODO: move this to a more central place (probably to ChangeSingleTripModeModule - after tests).
class ScoreMassConservationControlerListener implements BeforeMobsimListener {

	private final Population population;
	private final EventsManager eventsManager;
	private final HamburgExperimentalConfigGroup hamburgCfg;
	final String MASS_CONSERVATION_VIOLATION_SCORE_EVENT_KIND = "massConservationViolation";

	private List<String[]> statistics = new ArrayList<>();

	@Inject
	public ScoreMassConservationControlerListener(Config config, Population population, EventsManager eventsManager) {
		this.population = population;
		this.eventsManager = eventsManager;
		this.hamburgCfg = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent e) {
		Preconditions.checkArgument(hamburgCfg.getScorePenaltyForMassConservationViolation() <= 0, "score penalty for mass conservation must be negative or zero");

		Map<Plan, Collection<TripStructureUtils.Subtour>> person2Subtours = StreamEx.of(this.population.getPersons().values())
				.mapToEntry(person -> person.getSelectedPlan(), person -> TripStructureUtils.getSubtours(person.getSelectedPlan()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<Plan, Integer> person2Violations = EntryStream.of(person2Subtours)
				.mapValues(subtours -> getNumberOfMassConservationViolations(subtours))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		//throw PersonScoreEvents
		for (Map.Entry<Plan, Integer> planIntegerEntry : person2Violations.entrySet()) {
			if(planIntegerEntry.getValue() > 0) {
				eventsManager.processEvent(new PersonScoreEvent(0,
						planIntegerEntry.getKey().getPerson().getId(), planIntegerEntry.getValue() * hamburgCfg.getScorePenaltyForMassConservationViolation(),
						MASS_CONSERVATION_VIOLATION_SCORE_EVENT_KIND));
			}
		}

		String fileName = e.getServices().getControlerIO().getOutputFilename("subtourMassConservationViolations.tsv");

		int totalViolations = person2Violations.values().stream()
				.collect(Collectors.summingInt(Integer::intValue));

		long violatingPlans = person2Violations.entrySet().stream()
				.filter(entry -> entry.getValue() > 0)
				.count();

		int totalSubtours = person2Subtours.entrySet().stream()
				.map(entry -> entry.getValue().size())
				.collect(Collectors.summingInt(Integer::intValue));

		this.statistics.add(new String[]{String.valueOf(e.getIteration()),
				String.valueOf(person2Subtours.size()),
				String.valueOf( ( (double) violatingPlans / (double) person2Subtours.size() ) ),
				String.valueOf(totalSubtours),
				String.valueOf( (double) totalViolations / (double) totalSubtours )
		});

		String[] header = new String[]{"iteration", "nrOfSelectedPlans", "shareOfSelectedPlansWithMassConservationViolations", "nrOfSubtours", "shareOfSubtoursViolatingMassConservation" };
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(fileName),
					'\t',
					ICSVWriter.NO_QUOTE_CHARACTER,
					ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
					ICSVWriter.DEFAULT_LINE_END);
			writer.writeNext(header);
			writer.writeAll(statistics);
			writer.flush();
			writer.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	private int getNumberOfMassConservationViolations(Collection<TripStructureUtils.Subtour> subtours) {
		return (int) subtours.stream()
				.filter(subTour -> !AnalysePlansForSubtourModeChoice.isMassConserving(subTour))
				.count();
	}

}
