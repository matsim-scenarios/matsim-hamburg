package org.matsim.run;


import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.matsim.analysis.PlanBasedTripsFileWriter;
import org.matsim.analysis.PlanBasedTripsWriterControlerListener;
import org.matsim.analysis.here.HereAPIControlerListener;
import org.matsim.analysis.here.HereAPITravelTimeValidation;
import org.matsim.analysis.here.HereAPITravelTimeValidationConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.parking.NetworkParkPressureReader;
import org.matsim.parking.UtilityBasedParkingPressureEventHandler;
import org.matsim.prepare.freight.AdjustScenarioForFreight;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * @author zmeng
 */
public class RunBaseCaseWithMobilityBudgetV2 {

    private static final Logger log = Logger.getLogger(RunBaseCaseHamburgScenario.class);

    public static final String COORDINATE_SYSTEM = "EPSG:25832";
    public static final String VERSION = "v1.1";
    public static final double[] X_EXTENT = new double[]{490826.5738238178, 647310.6279172485};
    public static final double[] Y_EXTENT = new double[]{5866434.167201331, 5996884.970634732};
    public static final HashMap<Id<Person>, Double > personsWithMobilityBudget = new HashMap<>();
    public static double totalSumMobilityBudget = 0;
    static double dailyMobilityBudget;

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"scenarios/input/hamburg-v1.1-10pct.config.xml"};
        }

        RunBaseCaseWithMobilityBudgetV2.run(args);
    }

    private static void run(String[] args) throws IOException {

        Config config = prepareConfig(args);
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);
        controler.run();
        log.info("Done.");
    }


    public static Controler prepareControler(Scenario scenario) {

        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);

        MobilityBudgetEventHandler mobilityBudgetEventHandler = new MobilityBudgetEventHandler();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(mobilityBudgetEventHandler);
            }
        });

        controler.addControlerListener(new AfterMobsimListener() {
            @Override
            public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
                for (Id<Person> personId : personsWithMobilityBudget.keySet()) {
                    if (personsWithMobilityBudget.get(personId)>0) {
                        log.info("Throwing money event" + "Person_Id:" + personId);
                        afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, personId, personsWithMobilityBudget.get(personId), null, null));
                        totalSumMobilityBudget = totalSumMobilityBudget + personsWithMobilityBudget.get(personId);
                    }
                }
            }

        });

        log.info("This iteration the totalSumMobilityBudget paid to the Agents was:" + totalSumMobilityBudget);

        return controler;
    }

    public static Scenario prepareScenario(Config config) throws IOException {

        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        //ScenarioUtils.loadScenario(scenario);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            personsWithMobilityBudget.put(person.getId(), dailyMobilityBudget);
        }

        return scenario;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {

        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);
        dailyMobilityBudget = Double.parseDouble(args[6]);
        log.info(dailyMobilityBudget);
        return config;
    }
}

