package org.matsim.analysis.emissions;

import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.*;
import org.matsim.core.events.EventsUtils;

import java.lang.reflect.Parameter;

public class CollectCo2Emissions {

	public static void main(String[] args) {

		var manager = EventsUtils.createEventsManager();
		var handler = new EmissionCollector();
		manager.addHandler(handler);
		new EmissionEventsReader(manager).readFile("C:\\Users\\Janekdererste\\Projects\\matsim-hamburg\\scenarios\\output\\output-hamburg-v3.0-1pctemission-analysis-hbefa-v4.1-2020\\hamburg-v3.0-1pct-base.emission.events.offline.xml.gz");

		System.out.println("Collected " + handler.getAccumulatedEmissions() + "g CO2");
	}

	private static class EmissionCollector implements ColdEmissionEventHandler, WarmEmissionEventHandler {

		public double getAccumulatedEmissions() {
			return accumulatedEmissions;
		}

		private double accumulatedEmissions = 0.;

		@Override
		public void handleEvent(ColdEmissionEvent event) {
			var co2 = event.getColdEmissions().get(Pollutant.CO2_TOTAL);
			accumulatedEmissions += co2;
		}

		@Override
		public void handleEvent(WarmEmissionEvent event) {
			var co2 = event.getWarmEmissions().get(Pollutant.CO2_TOTAL);
			accumulatedEmissions += co2;
		}
	}
}