package org.matsim.pt;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class PtFromEventsFile {

    public static void main(String[] args) {

        String eventsFile = "D:/Arbeit/Hamburg/hh-10pct-19.output_events.xml";
        EventsManager eventsManager = EventsUtils.createEventsManager();
        MyEventHandler myEventHandler = new MyEventHandler();
        eventsManager.addHandler(myEventHandler);
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(eventsFile);


    }

}
