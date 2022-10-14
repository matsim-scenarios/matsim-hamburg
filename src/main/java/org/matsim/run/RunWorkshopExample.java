package org.matsim.run;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RunWorkshopExample {

	private static final Logger log = LogManager.getLogger(RunWorkshopExample.class);

	public static void main(String[] args) throws IOException {

		var config = RunBaseCaseHamburgScenario.prepareConfig(args);
		var scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
		var controler = RunBaseCaseHamburgScenario.prepareControler(scenario);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(ElectricityEventsHandler.class).in(Singleton.class);
				bind(ElectricGridPrice.class).in(Singleton.class);
				addEventHandlerBinding().to(ElectricityEventsHandler.class);
				addEventHandlerBinding().to(ElectricGridPrice.class);
				bindScoringFunctionFactory().to(ElectricityScoringFunctionFactory.class);
			}
		});



		controler.run();
	}

	private static class ElectricityScoringFunctionFactory implements ScoringFunctionFactory {

		@Inject
		private Scenario scenario;

		@Inject
		private ElectricityEventsHandler handler;

		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			SumScoringFunction sumScoringFunction = new SumScoringFunction();

			// Score activities, legs, payments and being stuck
			// with the default MATSim scoring based on utility parameters in the config file.
			final ScoringParameters params =
					new ScoringParameters.Builder(scenario, person).build();
			sumScoringFunction.addScoringFunction(new SumScoringFunction.ActivityScoring() {

				@Override
				public void finish() {

				}

				@Override
				public double getScore() {

					if (handler.electricityScore.containsKey(person.getId())) {
						log.info("Scoring!!! " + handler.electricityScore.get(person.getId()).score);
						return handler.electricityScore.get(person.getId()).score;
					}
					else
						return 0;
				}

				@Override
				public void handleFirstActivity(Activity act) {
					//nothing to do
				}

				@Override
				public void handleActivity(Activity act) {
					//nothing to do
				}

				@Override
				public void handleLastActivity(Activity act) {
					//nothing to do
				}
			});
			sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
			sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
			sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
			return sumScoringFunction;
		}
	}

	private static class ElectricityEventsHandler implements ActivityStartEventHandler, ActivityEndEventHandler, BasicEventHandler {

		private static final Id<Person> personOfInterest = Id.createPersonId("100046");

		@Inject
		EventsManager events;

		private final Set<Id<Person>> personsAtActivity = new HashSet<>();
		private final Map<Id<Person>, ElectricPerson> electricityScore = new HashMap<>();

		@Override
		public void handleEvent(Event event) {
			if (ElectricityPriceEvent.TYPE.equals(event.getEventType())) {
				var epe = (ElectricityPriceEvent) event;
				var nextState = epe.getPrice() > 1000 ? ElectricityState.FROM_COMBUSTION : ElectricityState.FROM_LANDLINE;

				for (var id : personsAtActivity) {
					var person = electricityScore.get(id);
					if (person.getState() != nextState) {
						person.setState(nextState);
						log.info("-------------------- Electricity Event ---------------------------");
						events.processEvent(new ElectricityStateChanged(event.getTime(), id, nextState));
					}
				}
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (!event.getPersonId().equals(personOfInterest)) return;

			personsAtActivity.remove(event.getPersonId());

			if (!electricityScore.containsKey(event.getPersonId())) return;

			var electricPerson = electricityScore.get(event.getPersonId());
			if (electricPerson.getState() == ElectricityState.FROM_COMBUSTION){
				electricPerson.setScore(-1000);
			} else {
				electricPerson.setScore(500);
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (!event.getPersonId().equals(personOfInterest)) return;

			personsAtActivity.add(event.getPersonId());
			electricityScore.computeIfAbsent(event.getPersonId(), id -> new ElectricPerson());
		}
	}

	private static class ElectricPerson {

		private ElectricityState state;
		private double score = 0;

		public ElectricityState getState() {
			return state;
		}

		public void setState(ElectricityState state) {
			this.state = state;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}
	}

	private static class ElectricGridPrice implements ActivityStartEventHandler, ActivityEndEventHandler {

		@Inject
		EventsManager events;

		private double price = 0;

		@Override
		public void handleEvent(ActivityEndEvent event) {

			var newPrice = Math.max(0, price - 1);
			if (newPrice != price) {
				events.processEvent(new ElectricityPriceEvent(event.getTime(), newPrice));
				this.price = newPrice;
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {

			var newPrice = price + 1;
			events.processEvent(new ElectricityPriceEvent(event.getTime(), newPrice));
			this.price = newPrice;
		}
	}

	private static class ElectricityPriceEvent extends Event {

		public static final String TYPE = "electricity_price";

		private final double price;

		public double getPrice() {
			return price;
		}

		public ElectricityPriceEvent(double time, double price) {
			super(time);
			this.price = price;
		}

		@Override
		public String getEventType() {
			return TYPE;
		}
	}

	private enum ElectricityState { FROM_COMBUSTION, FROM_LANDLINE };

	private static class ElectricityStateChanged extends Event {

		public static final String TYPE = "electricity_state";

		private final ElectricityState state;
		private final Id<Person> personId;

		public ElectricityState getState() {
			return state;
		}

		public Id<Person> getPersonId() {
			return personId;
		}

		public ElectricityStateChanged(double time, Id<Person> personId, ElectricityState changedTo) {
			super(time);
			this.state = changedTo;
			this.personId = personId;
		}

		@Override
		public String getEventType() {
			return TYPE;
		}
	}
}