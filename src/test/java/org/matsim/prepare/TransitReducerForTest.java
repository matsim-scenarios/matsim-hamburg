package org.matsim.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zmeng, haowu
 */
public class TransitReducerForTest {

    private static final Logger log = Logger.getLogger(TransitReducerForTest.class);

    public static void main(String[] args) throws IOException {

        /*String PTLinksID = "src/test/java/org/matsim/prepare/PTLinksID.txt";
        String TransitVehiclesID = "src/test/java/org/matsim/prepare/TransitVehiclesID.txt";
        String TransitStopsID = "src/test/java/org/matsim/prepare/TransitStopsID.txt";

        BufferedReader bfrPTLinksIDList = new BufferedReader(new FileReader(PTLinksID));
        ArrayList<String> PTLinksIDList = new ArrayList<>();
        while (true){
            String s = bfrPTLinksIDList.readLine();
            if(s==null){
                break;
            }
            PTLinksIDList.add(s);
        }
        bfrPTLinksIDList.close();
        System.out.println(PTLinksIDList);
        System.out.println(PTLinksIDList.size());

        BufferedReader bfrTransitVehiclesIDList = new BufferedReader(new FileReader(TransitVehiclesID));
        ArrayList<String> TransitVehiclesIDList = new ArrayList<>();
        while (true){
            String s = bfrTransitVehiclesIDList.readLine();
            if(s==null){
                break;
            }
            TransitVehiclesIDList.add(s);
        }
        bfrTransitVehiclesIDList.close();
        System.out.println(TransitVehiclesIDList);
        System.out.println(TransitVehiclesIDList.size());

        BufferedReader bfrTransitStopsIDList = new BufferedReader(new FileReader(TransitStopsID));
        ArrayList<String> TransitStopsIDList = new ArrayList<>();
        while (true){
            String s = bfrTransitStopsIDList.readLine();
            if(s==null){
                break;
            }
            TransitStopsIDList.add(s);
        }
        bfrTransitStopsIDList.close();
        System.out.println(TransitStopsIDList);
        System.out.println(TransitStopsIDList.size());*/




//        Config config = ConfigUtils.createConfig();
//
//        config.network().setInputFile("test/input/Test/test-hamburg-v1.1-network-with-pt.xml.gz");
//        config.transit().setTransitScheduleFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1-transitSchedule.xml.gz");
//        config.transit().setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1-transitVehicles.xml.gz");

        Config config = ConfigUtils.loadConfig("test/input/Test/test-hamburg.config.xml");

        Scenario scenario = ScenarioUtils.loadScenario(config);

//        // ---change network
///*        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
//        log.info("read network from " + config.network().getInputFile());
//        networkReader.readFile(config.network().getInputFile());*/
//        List<Link> reLink = new LinkedList<>();
//        for(Link tr: scenario.getNetwork().getLinks().values()){
//            if(!PTLinksIDList.contains(tr.getId().toString())){
//                reLink.add(tr);
//            }
//            //System.out.println( "1" );
//        }
//
//        for (Link tr : reLink) {
//            scenario.getNetwork().removeLink(tr.getId());
//        }
//
//        NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
//        networkWriter.write("test/input/Test/WithLessPTLines/test-hamburg-v1.1-network-with-pt.xml.gz");

        // ---change transit schedule
        List<TransitLine> reTrs = new LinkedList<>();
        for(TransitLine tr: scenario.getTransitSchedule().getTransitLines().values()){
            if(!tr.getId().equals(Id.create("S1---5111_109",TransitLine.class))){
                reTrs.add(tr);
            }
            //System.out.println( "1" );
        }

        for (TransitLine tr : reTrs) {
            scenario.getTransitSchedule().removeTransitLine(tr);
        }

        /*List<TransitRoute> reTrsRoute = new LinkedList<>();
        for(TransitLine tr: scenario.getTransitSchedule().getTransitLines().values()){
            if(tr.getId().equals(Id.create("S1---5111_109",TransitLine.class))){
                for(TransitRoute trRoute: tr.getRoutes().values()) {
                    if(!trRoute.getId().equals(Id.create("S1---5111_109_7",TransitRoute.class))) {
                        reTrsRoute.add(trRoute);
                    }
                }
            }
            //System.out.println( "1" );
        }

        for (TransitRoute tr : reTrsRoute) {
            scenario.getTransitSchedule().getTransitLines().;
        }*/

/*        //---change transit stops
        List<TransitStopFacility> reTrsStop = new LinkedList<>();
        for(TransitStopFacility tr: scenario.getTransitSchedule().getFacilities().values()){
            if(!TransitStopsID.contains(tr.getId().toString())){
                reTrsStop.add(tr);
            }
            //System.out.println( "1" );
        }

        for (TransitStopFacility tr : reTrsStop) {
            scenario.getTransitSchedule().removeStopFacility(tr);
        }*/

        TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(scenario.getTransitSchedule());
        transitScheduleWriter.writeFile("test/input/Test/WithLessPTLines/hamburg-v1-transitSchedule.xml.gz");

/*        //---change transit vehicle
        List<Vehicle> reTrsVeh = new LinkedList<>();
        for(Vehicle tr: scenario.getTransitVehicles().getVehicles().values()){
            if(!TransitVehiclesID.contains(tr.getId().toString())){
                reTrsVeh.add(tr);
            }
            //System.out.println( "1" );
        }

        for (Vehicle tr : reTrsVeh) {
            scenario.getTransitVehicles().removeVehicle(tr.getId());
        }

        MatsimVehicleWriter transitVehiclesWriter = new MatsimVehicleWriter(scenario.getTransitVehicles());
        transitVehiclesWriter.writeFile("test/input/Test/WithLessPTLines/hamburg-v1-transitVehicles.xml.gz");*/

    }
}
