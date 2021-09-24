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
import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

//TODO potentially rename to MassConservationScoringModule
public class PenalizeMassConservationViolationsModule extends AbstractModule {

	@Override
	public void install() {
		boolean strategyConfigured = getConfig().strategy().getStrategySettings().stream()
				.filter(settings -> settings.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode))
				.findAny()
				.isPresent();
		Preconditions.checkArgument(strategyConfigured, "The module " + this.getClass() + " is designed to be used with " + DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode +
				" only. Please assure that you configure that strategy in the config file!");

		bind(ScoreMassConservationControlerListener.class).in(Singleton.class);
		addControlerListenerBinding().to(ScoreMassConservationControlerListener.class);
	}
}
