package org.matsim.analysis.mobBug;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class TravelTimeAnalysis  {



    public static void main(String args[]) throws IOException {

        //String eventFile = "D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\MATSim\\Outputs\\0.5\\hamburg-v1.1-10pct.output_events.xml.gz";
        String eventFile ="test/output/org/matsim/run/RunHamburgScenarioTest/runTest/runTest.output_events.xml.gz";
        EventsManager events = EventsUtils.createEventsManager();
        TravelTimeEventHandler travelTimeAnalysis = new TravelTimeEventHandler();
        events.addHandler(travelTimeAnalysis);
        new MatsimEventsReader(events).readFile(eventFile);
        writeResults2CSV(travelTimeAnalysis.getTravelTimes());
    }

    private static void writeResults2CSV (HashMap<Id<Person>, Double> personTripTime ) throws IOException {
        FileWriter writer = new FileWriter("D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\test.csv");
        writer.write("carLink"+";"+"ptLink"+";"+"amountOfCrossings");
        writer.append("\n");

        for (Id<Person> p: personTripTime.keySet()) {
            writer.append(p+","+personTripTime.get(p));
            writer.append("\n");
        }
        writer.close();
    }

}
