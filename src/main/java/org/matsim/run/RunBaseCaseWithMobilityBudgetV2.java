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
    static boolean useIncomeForMobilityBudget;
    static double shareOfIncome;


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
        log.info("Total paid sum MobBug: " + totalSumMobilityBudget);
        log.info("Done.");
    }


    public static Controler prepareControler(Scenario scenario) {
        log.info("Preparing controler");
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
        log.info("using income"+ useIncomeForMobilityBudget);
        log.info("share of income"+shareOfIncome);
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();

                //TripStructureUtil get Legs
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                    }

                }
                //@TS this before was if not contains didnt make sense to me why was that?
                if (transportModeList.contains(TransportMode.car)) {
                    personsEligibleForMobilityBudget.put(personId, dailyMobilityBudget);
                }
            }
        }

        if (useIncomeForMobilityBudget == true) {
            log.info("using the income for the MobilityBudget");
            for (Id<Person> personId : personsEligibleForMobilityBudget.keySet()) {
                //divided by 30 because income is needed per day
                double incomeOfAgent = (double) scenario.getPopulation().getPersons().get(personId).getAttributes().getAttribute("income")/30;
                dailyMobilityBudget = incomeOfAgent * shareOfIncome;
                personsEligibleForMobilityBudget.replace(personId, dailyMobilityBudget);
            }
        }
        return scenario;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        log.info("Preparing config");

        useIncomeForMobilityBudget = Boolean.parseBoolean(args[8]);
        log.info("using income for mobilityBudget: "+ useIncomeForMobilityBudget);
        shareOfIncome =Double.parseDouble(args[10]);
        log.info("share of income: "+ shareOfIncome);


        try {
            dailyMobilityBudget = Double.parseDouble(args[6]);
        } catch (NumberFormatException numberFormatException) {
            log.warn("Setting dailyMobilityBudget to default of 10.0");
            dailyMobilityBudget = 10.0;
        } catch (NullPointerException nullPointerException) {
            log.warn("Setting dailyMobilityBudget to default of 10.0");
            dailyMobilityBudget = 10.0;
        }

        log.info(dailyMobilityBudget);


        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);


       /* try {
            useIncomeForMobilityBudget = Boolean.parseBoolean(args[8]);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Not using income for the MobilityBudget");
            useIncomeForMobilityBudget = false;
        }
        catch (NullPointerException nullPointerException) {
            log.warn("Not using income for the MobilityBudget");
            useIncomeForMobilityBudget = false;
        }

        try {
            shareOfIncome =Double.parseDouble(args[10]);
        }
        catch (NumberFormatException numberFormatException) {
            log.warn("Using default share of income for the MobilityBudget");
            shareOfIncome = 0.10;
        }
        catch (NullPointerException nullPointerException) {
            log.warn("Using default share of income for the MobilityBudget");
            shareOfIncome = 0.10;
        }*/

        return config;
    }
}

