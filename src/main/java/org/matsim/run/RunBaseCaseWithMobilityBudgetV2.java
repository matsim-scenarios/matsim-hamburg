package org.matsim.run;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author zmeng
 */
public class RunBaseCaseWithMobilityBudgetV2 {

    private static final Logger log = Logger.getLogger(RunBaseCaseHamburgScenario.class);

    public static final String COORDINATE_SYSTEM = "EPSG:25832";
    public static final String VERSION = "v1.1";
    public static final double[] X_EXTENT = new double[]{490826.5738238178, 647310.6279172485};
    public static final double[] Y_EXTENT = new double[]{5866434.167201331, 5996884.970634732};
    //public static final HashMap<Id<Person>, Double > personsWithMobilityBudget = new HashMap<>();
    public static final Map<Id<Person>, Double> personsEligibleForMobilityBudget = new HashMap<>();
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
        log.info("Total paid sum MobBug: "+totalSumMobilityBudget);
        log.info("Done.");
    }


    public static Controler prepareControler(Scenario scenario) {

        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);
        MobilityBudgetEventHandlerV2 mobilityBudgetEventHandler = new MobilityBudgetEventHandlerV2(personsEligibleForMobilityBudget);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(mobilityBudgetEventHandler);
                addControlerListenerBinding().toInstance(mobilityBudgetEventHandler);
            }
        });
        return controler;
    }

    public static Scenario prepareScenario(Config config) throws IOException {

        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        log.info("filtering population for mobilityBudget");

        for (Person person : scenario.getPopulation().getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();

                //TripStructureUtil get Legs
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    List<String> transportModeList = new ArrayList<>();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                    }
                    if (!transportModeList.contains(TransportMode.car)) {
                        personsEligibleForMobilityBudget.put(personId, dailyMobilityBudget);
                    }
                }
            }
        }
        return scenario;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);

        try {
            dailyMobilityBudget = Double.parseDouble(args[6]);
        }
        catch (NumberFormatException numberFormatException) {
            dailyMobilityBudget = 10.0;
        }
        catch (NullPointerException nullPointerException) {
            dailyMobilityBudget = 10.0;
        }
        log.info(dailyMobilityBudget);
        return config;
    }
}

