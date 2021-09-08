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

package org.matsim.analysis.sharing;

import org.matsim.contrib.sharing.service.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;


public class SharingEventsReader {
	public static MatsimEventsReader create(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);

		reader.addCustomEventMapper(SharingPickupEvent.TYPE, SharingPickupEvent::convert);
		reader.addCustomEventMapper(SharingFailedPickupEvent.TYPE, SharingFailedPickupEvent::convert);
		reader.addCustomEventMapper(SharingFailedDropoffEvent.TYPE, SharingFailedDropoffEvent::convert);
		reader.addCustomEventMapper(SharingDropoffEvent.TYPE, SharingDropoffEvent::convert);
		reader.addCustomEventMapper(SharingVehicleEvent.TYPE, SharingVehicleEvent::convert);

		return reader;
	}
}
