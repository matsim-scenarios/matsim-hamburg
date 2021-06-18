package org.matsim.run;

import com.google.inject.Guice;
import com.google.inject.name.Names;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.sharingFare.SharingFareHandler;
import org.matsim.sharingFare.SharingFaresConfigGroup;
import org.matsim.sharingFare.SharingServiceFaresConfigGroup;
import org.matsim.sharingFare.TeleportedSharingFareHandler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.RunSharingScenario.*;

/**
 * @author zmeng
 */
public class RunSharingScenarioTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        String args[] = new String[]{
                "test/input//test-hamburg.config.xml" ,
                "--config:controler.lastIteration" , "20",
                "--config:hamburgExperimental.freeSpeedFactor", "1.2",
                "--config:hamburgExperimental.usePersonIncomeBasedScoring", "false",
                "--config:HereAPITravelTimeValidation.useHereAPI","false",
                "--config:hamburgExperimental.useLinkBasedParkPressure","true",
                "--config:hamburgExperimental.parkPressureScoreConstant","-2.",
                "--config:plans.inputPlansFile" , "test-hamburg-freight.plans.xml"
        };

        Config config = prepareConfig(args);

        ConfigUtils.addOrGetModule(config, SharingFaresConfigGroup.class);

        config.controler().setRunId("runTest");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        for (StrategyConfigGroup.StrategySettings strategySettings : config.strategy().getStrategySettings()) {
            if (strategySettings.getStrategyName().equals("SubtourModeChoice") && strategySettings.getSubpopulation().equals("person"))
                strategySettings.setWeight(100);
        }

        ConfigUtils.addOrGetModule(config, SharingFaresConfigGroup.class);
        SharingFaresConfigGroup sharingFaresConfigGroup = ConfigUtils.addOrGetModule(config, SharingFaresConfigGroup.class);

        SharingServiceFaresConfigGroup sharingCarFares = new SharingServiceFaresConfigGroup();
        sharingCarFares.setId("car");
        sharingCarFares.setBasefare(5.);
        sharingCarFares.setTimeFare_m(3.);
        sharingCarFares.setDistanceFare_m(10.);

        sharingFaresConfigGroup.addParameterSet(sharingCarFares);


        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(AnalysisMainModeIdentifier.class).toInstance(new RoutingModeMainModeIdentifier());
            }
        });

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(new SharingFareHandler("car"));
                this.addEventHandlerBinding().toInstance(new TeleportedSharingFareHandler("bike"));
            }
        });

        controler.run();


    }
}
