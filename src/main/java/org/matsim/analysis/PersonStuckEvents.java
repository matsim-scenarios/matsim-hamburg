package org.matsim.analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import playground.vsp.analysis.modules.stuckAgents.GetStuckEvents;

public class PersonStuckEvents {

    public static void  main(String args[]) {

        String eventsFile = "D:/Gregor/Uni/TUCloud/Masterarbeit/MATSim/Outputs/FixedValue/Test/h-v2-10pct-accEcc-c4.output_events.xml.gz";
        String outputFile="D:/Gregor/Uni/TUCloud/Masterarbeit/MATSim/Outputs/FixedValue/Test/numberOfStuckEvents";
        EventsManager eventsManager = EventsUtils.createEventsManager();
        GetStuckEvents stuckEventHandler = new GetStuckEvents();
        eventsManager.addHandler(stuckEventHandler);
        stuckEventHandler.writeResults(outputFile);
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        stuckEventHandler.writeResults(outputFile);
    }

}
