package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClassifyStationType {
    public static void main(String[] args) throws IOException {
        String check = "no other transport mode!";
        String check2 = "nothing wrong!";

        List<TransitStopFacility> StopList_bus = new ArrayList<>();
        List<TransitStopFacility> StopList_subway = new ArrayList<>();
        List<TransitStopFacility> StopList_rail = new ArrayList<>();

        List<Stop2type> stops = new LinkedList<>();

//        List<TransitStopFacility> StopList_rsb = new ArrayList<>();
//        List<TransitStopFacility> StopList_rs = new ArrayList<>();
//        List<TransitStopFacility> StopList_rb = new ArrayList<>();
//        List<TransitStopFacility> StopList_sb = new ArrayList<>();
//        List<TransitStopFacility> StopList_r = new ArrayList<>();
//        List<TransitStopFacility> StopList_s = new ArrayList<>();
//        List<TransitStopFacility> StopList_b = new ArrayList<>();

        String configFile = "scenarios/input/hamburg-v1.1-1pct.config.xml";
        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        //save stops respectively
        TransitSchedule transitSchedule = scenario.getTransitSchedule();
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
                        check = "There is other transport mode!";

                    }
                }

            }
        }
        System.out.println("For Check: "+check);

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
//            if(StopList_rail.contains(transitStopFacility)&&StopList_subway.contains(transitStopFacility)&&StopList_bus.contains(transitStopFacility)){
//                StopList_rsb.add(transitStopFacility);
//
//            }else if(StopList_rail.contains(transitStopFacility)&&StopList_subway.contains(transitStopFacility)&&(!StopList_bus.contains(transitStopFacility))){
//                StopList_rs.add(transitStopFacility);
//
//            }else if(StopList_rail.contains(transitStopFacility)&&(!StopList_subway.contains(transitStopFacility))&&StopList_bus.contains(transitStopFacility)){
//                StopList_rb.add(transitStopFacility);
//
//            }else if((!StopList_rail.contains(transitStopFacility))&&StopList_subway.contains(transitStopFacility)&&StopList_bus.contains(transitStopFacility)){
//                StopList_sb.add(transitStopFacility);
//
//            }else if(StopList_rail.contains(transitStopFacility)&&(!StopList_subway.contains(transitStopFacility))&&(!StopList_bus.contains(transitStopFacility))){
//                StopList_r.add(transitStopFacility);
//
//            }else if((!StopList_rail.contains(transitStopFacility))&&StopList_subway.contains(transitStopFacility)&&(!StopList_bus.contains(transitStopFacility))){
//                StopList_s.add(transitStopFacility);
//
//            }else if((!StopList_rail.contains(transitStopFacility))&&(!StopList_subway.contains(transitStopFacility))&&StopList_bus.contains(transitStopFacility)){
//                StopList_b.add(transitStopFacility);
//
//            }else{
//                check2 = "There is some problem! Maybe some stations are not used and they just exits there!";
//
//            }
        }
        System.out.println("For Check2: "+check2);

        //print out
        FileWriter out = new FileWriter( new File( "out.csv" ) );
        //out.write( "X;Y\n" );
        //out.write( xx + ";" + yy + ";\n" );
        out.write("id;x;y;name;stationType\n");

        for (Stop2type stop : stops) {
            out.write(stop.transitStopFacility.getId().toString() + ";" + stop.transitStopFacility.getCoord().getX() + ";" + stop.transitStopFacility.getCoord().getY() + ";" + stop.transitStopFacility.getName().toString() + ";" + stop.getType() + "\n");
        }
//        for(TransitStopFacility transitStopFacility: StopList_rsb){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "rsb\n" );
//        }
//        for(TransitStopFacility transitStopFacility: StopList_rs){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "rs\n" );
//        }
//        for(TransitStopFacility transitStopFacility: StopList_rb){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "rb\n" );
//        }
//        for(TransitStopFacility transitStopFacility: StopList_sb){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "sb\n" );
//        }
//        for(TransitStopFacility transitStopFacility: StopList_r){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "r\n" );
//        }
//        for(TransitStopFacility transitStopFacility: StopList_s){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "s\n" );
//        }
//        for(TransitStopFacility transitStopFacility: StopList_b){
//            out.write( transitStopFacility.getId().toString() + ";" + transitStopFacility.getCoord().getX() + ";" + transitStopFacility.getCoord().getY() + ";" + transitStopFacility.getName().toString() + ";" + "s\n" );
//        }
        out.close();
    }

    private static class Stop2type{
        TransitStopFacility transitStopFacility;
        String type;
        Stop2type(TransitStopFacility transitStopFacility) {
            this.transitStopFacility = transitStopFacility;
        }

        public void addType(String type) {
            if(this.type == null)
                this.type = type;
            else
                this.type = this.type + type;
        }

        public String getType() {
            return type;
        }
    }
}
