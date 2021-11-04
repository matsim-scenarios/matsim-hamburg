package org.matsim.prepare.pt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClassifyStationType {
    private static final Logger log = Logger.getLogger(ClassifyStationType.class);

    public static void main(String[] args) throws IOException {



        String configFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.2/input/baseCase/hamburg-v2.2-10pct.config.baseCase.xml";
        Config config = ConfigUtils.loadConfig(configFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);


        //save stops respectively
        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        List<Stop2type> stops = getStop2TypesList(transitSchedule);

        writeOutput(stops);
    }

    public static List<Stop2type> getStop2TypesList(TransitSchedule transitSchedule) {
        List<Stop2type> stops = new LinkedList<>();
        List<TransitStopFacility> StopList_bus = new ArrayList<>();
        List<TransitStopFacility> StopList_subway = new ArrayList<>();
        List<TransitStopFacility> StopList_rail = new ArrayList<>();

        int wrnCnt = 5;
        for(TransitLine transitLine: transitSchedule.getTransitLines().values()){
            for (TransitRoute transitRoute : transitLine.getRoutes().values()){
                for(TransitRouteStop stop :transitRoute.getStops()){
                    if(transitRoute.getTransportMode().equals("rail")){
                        if(!StopList_rail.contains(stop)){
                            StopList_rail.add(stop.getStopFacility());
                            //System.out.println("rail!");
                        }

                    }else if(transitRoute.getTransportMode().equals("subway")){
                        if(!StopList_subway.contains(stop)){
                            StopList_subway.add(stop.getStopFacility());
                            //System.out.println("subway!");
                        }

                    }else if(transitRoute.getTransportMode().equals("bus")){
                        if(!StopList_bus.contains(stop)){
                            StopList_bus.add(stop.getStopFacility());
                            //System.out.println("bus!");
                        }

                    }else{
                        if(wrnCnt > 0){
                            log.warn("There is another transport mode: " + transitRoute.getTransportMode() + " at station " + stop);
                            wrnCnt --;
                            if(wrnCnt == 0) {
                                log.warn("future warning messages of this type are suppressed");
                            }
                        }
                    }
                }

            }
        }

        //prepare for stopType
        for(TransitStopFacility transitStopFacility: transitSchedule.getFacilities().values()) {
            Stop2type stop2type = new Stop2type(transitStopFacility);
            if(StopList_rail.contains(transitStopFacility)){
                stop2type.addType("r");
            }
            if(StopList_bus.contains(transitStopFacility)){
                stop2type.addType("b");
            }
            if(StopList_subway.contains(transitStopFacility)){
                stop2type.addType("s");
            }

            stops.add(stop2type);

        }
        return stops;
    }

    private static void writeOutput(List<Stop2type> stops) throws IOException {
        //print out
        FileWriter out = new FileWriter("transitStationTypes_2030.csv");
        //out.write( "X;Y\n" );
        //out.write( xx + ";" + yy + ";\n" );
        out.write("id;x;y;name;stationType\n");

        for (Stop2type stop : stops) {
            out.write(stop.transitStopFacility.getId().toString() + ";" + stop.transitStopFacility.getCoord().getX() + ";" + stop.transitStopFacility.getCoord().getY() + ";" + stop.transitStopFacility.getName() + ";" + stop.getType() + "\n");
        }

        out.close();
    }

    //should be replaced by a Map<TransitStopFacility,String> or even something cleverer
    public static class Stop2type{
        TransitStopFacility transitStopFacility;
        String type;
        Stop2type(TransitStopFacility transitStopFacility) {
            this.transitStopFacility = transitStopFacility;
        }

        void addType(String type) {
            if(this.type == null)
                this.type = type;
            else
                this.type = this.type + type;
        }

        @Nullable
        public String getType() {
            return type;
        }

        public TransitStopFacility getStop() {
            return transitStopFacility;
        }
    }
}
