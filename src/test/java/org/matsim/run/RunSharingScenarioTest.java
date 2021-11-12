package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.matsim.run.RunSharingScenario.*;

/**
 * @author zmeng
 */
public class RunSharingScenarioTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test

    public void runTest() throws IOException {

        String[] args = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "0",
                "--config:controler.runId" , "RunSharingScenarioTest",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:hamburgExperimental.carSharingServiceInputFile", "shared_car_vehicles_stations.xml",
                "--config:hamburgExperimental.bikeSharingServiceInputFile", "shared_bike_vehicles_stations.xml",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:network.inputNetworkFile" , "test-hamburg-with-pt-network.xml.gz",
                "--config:plans.inputPlansFile" , "./plans/test-scar-sharing-user.plans.xml"
        };

        Config config = prepareConfig(args);

        config.transit().setUseTransit(false);

        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setOutputDirectory(utils.getOutputDirectory());


//        SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config,SharingConfigGroup.class);
//        for (SharingServiceConfigGroup service : sharingConfigGroup.getServices()) {
//            switch (service.getMode()){
//                case "sbike":
//                    service.setServiceInputFile("shared_bike_vehicles_stations.xml");
//                    break;
//                case "scar":
//                    service.setServiceInputFile("shared_car_vehicles_stations.xml");
//                    break;
//                default:
//                    throw new IllegalArgumentException();
//            }
//        }


        Scenario scenario = prepareScenario(config);

        //override parking pressure for link (destination of scarUser 2a and origin of scarUser2b)
        scenario.getNetwork().getLinks().get(Id.createLinkId("5170509070009r")).getAttributes().putAttribute("parkPressure", 1.0);

        Controler controler = prepareControler(scenario);

        StuckPersonHandler stuckPersonHandler = new StuckPersonHandler();
        ParkingPressureHandler parkingPressureHandler = new ParkingPressureHandler();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(stuckPersonHandler);
                this.addEventHandlerBinding().toInstance(parkingPressureHandler);
            }
        });
        controler.run();

        /*
         * Person scarUser_1a will drop the vehicle at 9:07:00 and scarUser_1b want to pick it up 1 seconds later at 9:07:01.
         * Person scarUser_2a will drop the vehicle at 9:25:50 and scarUser_2b want to pick it up at the very same moment 9:25:50.
         *
         * In the former case, both agent should be able to use sharing car. In the latter case, person scarUser_2b can not find
         * an idle car because the one in the near is still in use with person scarUser_2a.
         *
         * This test verified:
         *      agent using car-sharing can "take" the corresponding vehicle to the destination
         *      the vehicle cannot be found by other users when it is in use, at least 1 second is needed for placing it to an "idle" status
         */

        Assert.assertEquals(1, stuckPersonHandler.getStuckPersons().size());
        Assert.assertEquals("scarUser_2b", stuckPersonHandler.getStuckPersons().get(0).toString());


        /**
         * test that the car-sharing users also perceive parking pressure
         */
        Assert.assertEquals(1, parkingPressureHandler.parkPressureScoreEvents.size());
        Assert.assertEquals(1., parkingPressureHandler.parkPressureScoreEvents.get(0).getAmount(), 0.);

    }

    private class StuckPersonHandler implements PersonStuckEventHandler{
        List<Id<Person>> stuckPersons = new LinkedList<>();
        @Override
        public void handleEvent(PersonStuckEvent personStuckEvent) {
            if(personStuckEvent.getLegMode().equals("sharing:car")){
                stuckPersons.add(personStuckEvent.getPersonId());
            }
        }

        public List<Id<Person>> getStuckPersons() {
            return stuckPersons;
        }
    }

    private class ParkingPressureHandler implements PersonScoreEventHandler {
        List<PersonScoreEvent> parkPressureScoreEvents = new LinkedList<>();
        @Override
        public void handleEvent(PersonScoreEvent event) {
            if(event.getKind().equals("parkPressure")){
                parkPressureScoreEvents.add(event);
            }
        }

    }

}
