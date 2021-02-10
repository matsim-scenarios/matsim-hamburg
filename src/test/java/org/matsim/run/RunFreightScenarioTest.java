package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.here.HereAPIControlerListener;
import org.matsim.analysis.here.HereAPITravelTimeValidation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.prepare.freight.AdjustScenarioForFreight;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.matsim.run.RunBaseCaseHamburgScenario.*;

/**
 * @author zmeng
 */
public class RunFreightScenarioTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
                "test/input/test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "2",
                "--config:plans.inputPlansFile" , "test-hamburg-freight.plans.xml",
                "--config:vehicles.vehiclesFile" , "test-vehicle-types.xml"
        };

        Config config = prepareConfig(args);

        config.controler().setRunId("runFreightScenarioTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        List<String> modes = Arrays.asList("Lfw","Lkw-g","Trans","Pkw-Lfw","Lkw-k");
        String freight = "freight_";
        modes = modes.stream().map(mode -> freight + mode).collect(Collectors.toList());

        Scenario scenario = prepareScenario(config);
        AdjustScenarioForFreight.adjustScenarioForFreight(scenario,modes);

        Controler controler = prepareControler(scenario);
        AdjustScenarioForFreight.adjustControlerForFreight(controler,modes);

        controler.run();


    }

}
