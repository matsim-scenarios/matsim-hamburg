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
import org.matsim.prepare.SelectionMobilityBudget;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author zmeng,gryb
 */
public class RunBaseCaseWithMobilityBudget {

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
    public static boolean useShapeFile;
    static String shapeFile;
    public static boolean incomeBasedSelection;
    static double shareOfAgents;


    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"scenarios/input/hamburg-v1.1-10pct.config.xml"};
        }

        processArguments(args);
        RunBaseCaseWithMobilityBudget.run(args);
    }

    private static void run(String[] args) throws IOException {
        Config config = prepareConfig(args);
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);
        controler.run();
        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        log.info("Preparing controler");
        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);
        MobilityBudgetEventHandler mobilityBudgetEventHandler = new MobilityBudgetEventHandler(personsEligibleForMobilityBudget);
        addMobilityBudgetHandler(controler, mobilityBudgetEventHandler);
        return controler;
    }

    public static void addMobilityBudgetHandler(Controler controler, MobilityBudgetEventHandler mobilityBudgetEventHandler) {
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(mobilityBudgetEventHandler);
                addControlerListenerBinding().toInstance(mobilityBudgetEventHandler);
            }
        });
    }

    public static Scenario prepareScenario(Config config) throws IOException {

        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        for (Map.Entry<Id<Person>, Double> entry : getPersonsEligibleForMobilityBudget2FixedValue(scenario, dailyMobilityBudget).entrySet()) {
            Id<Person> person = entry.getKey();
            Double budget = entry.getValue();
            personsEligibleForMobilityBudget.put(person, budget);
        }

        log.info("using income "+ useIncomeForMobilityBudget);
        log.info("share of income "+ shareOfIncome);
        if (useIncomeForMobilityBudget == true) {
            log.info("using the income for the MobilityBudget");
            for (Id<Person> personId : personsEligibleForMobilityBudget.keySet()) {
                double monthlyIncomeOfAgent = (double) scenario.getPopulation().getPersons().get(personId).getAttributes().getAttribute("income")/12;
                //divided by 30 because income is needed per day
                double incomeOfAgent = monthlyIncomeOfAgent/30;
                dailyMobilityBudget = incomeOfAgent * shareOfIncome;
                personsEligibleForMobilityBudget.replace(personId, dailyMobilityBudget);
            }
        }

        if (useShapeFile == true) {
            log.info("Filtering for Region" + useShapeFile);
            SelectionMobilityBudget.filterForRegion(scenario.getPopulation(), shapeFile, personsEligibleForMobilityBudget );
        }

        if (incomeBasedSelection==true) {
            log.info("Selceting Agents based on Income " + incomeBasedSelection);
            SelectionMobilityBudget.incomeBasedSelection(scenario.getPopulation(),shareOfAgents, personsEligibleForMobilityBudget);
        }

        return scenario;
    }

    public static Map<Id<Person>, Double> getPersonsEligibleForMobilityBudget2FixedValue(Scenario scenario, Double value) {

        Map<Id<Person>, Double> persons2Budget = new HashMap<>();

        log.info("filtering population for mobilityBudget");

        for (Person person : scenario.getPopulation().getPersons().values()) {
            Id<Person> personId = person.getId();
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
                if (transportModeList.contains(TransportMode.car)) {
                   persons2Budget.put(personId, value);
                }
            }
        }
        return persons2Budget;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        log.info("Preparing config");
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);

        log.info("using income for mobilityBudget: "+ useIncomeForMobilityBudget);
        log.info("share of income: "+ shareOfIncome);
        log.info("use ShapeFile "+ useShapeFile);
        log.info("using income for mobilityBudget: "+ useIncomeForMobilityBudget);
        log.info("share of income: "+ shareOfIncome);
        log.info("use ShapeFile "+ useShapeFile);
        log.info("income based Selection "+incomeBasedSelection);
        log.info("agents share" + shareOfAgents);
        return config;
    }

    private static void processArguments(String[] args) {

        log.info("Processing arguments for MobBudget");
        dailyMobilityBudget = Double.parseDouble(args[6]);
        useIncomeForMobilityBudget = Boolean.parseBoolean(args[8]);
        shareOfIncome =Double.parseDouble(args[10]);
        useShapeFile = Boolean.parseBoolean(args[12]);
        shapeFile = args[14];
        incomeBasedSelection = Boolean.parseBoolean(args[16]);
        shareOfAgents = Double.parseDouble(args[18]);

        log.info("dailyMobBud "+dailyMobilityBudget);
        log.info("use Income " +useIncomeForMobilityBudget);
        log.info("share of income "+shareOfIncome);
        log.info("use shape File "+useShapeFile);
        log.info("income based Selection "+incomeBasedSelection);
        log.info(shareOfAgents +shareOfAgents);

    }
}

