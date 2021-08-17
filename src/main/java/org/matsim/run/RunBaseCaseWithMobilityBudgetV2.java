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
    public static boolean useShapeFile;
    //private static String shapeFile = "C:\\Users\\Gregor\\Documents\\shared-svn\\projects\\realLabHH\\data\\hamburg_shapeFile\\hamburg_metropo\\hamburg_metropo.shp";
    static String shapeFile;
    public static boolean incomeBasedSelection;
    static double shareOfAgents;


    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            //args = new String[] {"scenarios/input/hamburg-v1.1-10pct.config.xml"};
            args = new String[] {"D:\\Gregor\\Uni\\TUCloud\\Masterarbeit\\h-v2-10pct-accEcc-c4.output_config.xml"};

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
        log.info("using income "+ useIncomeForMobilityBudget);
        log.info("share of income "+shareOfIncome);
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
                if (transportModeList.contains(TransportMode.car)) {
                    personsEligibleForMobilityBudget.put(personId, dailyMobilityBudget);
                }
            }
        }

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

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        log.info("Preparing config");
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);

        log.info("using income for mobilityBudget: "+ useIncomeForMobilityBudget);
        log.info("share of income: "+ shareOfIncome);
        log.info("use ShapeFile "+ useShapeFile);

        processArguments(args);


        return config;
    }

    private static void processArguments(String[] args) {
        try {
            dailyMobilityBudget = Double.parseDouble(args[6]);
        } catch (NumberFormatException numberFormatException) {
            log.warn("Setting dailyMobilityBudget to default of 100.0");
            dailyMobilityBudget = 100.0;
        } catch (NullPointerException nullPointerException) {
            log.warn("Setting dailyMobilityBudget to default of 100.0");
            dailyMobilityBudget = 100.0;
        }
        catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            log.warn(arrayIndexOutOfBoundsException);
            log.warn("Setting dailyMobilityBudget to default of 100.0");
            dailyMobilityBudget = 100.0;
        }
        log.info(dailyMobilityBudget);
        try {
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
        catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
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
        }
        catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            log.warn(arrayIndexOutOfBoundsException);
            log.warn("Using default share of income for the MobilityBudget");
            shareOfIncome = 0.10;
        }

        try {
            useShapeFile = Boolean.parseBoolean(args[12]);
            log.info("Using shape File");
        }

        catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Not using shape File");
            useShapeFile = false;
        }

        try {
            shapeFile = args[14];
        }

        catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Not able to read Shape File");
            shapeFile = "C:\\Users\\Gregor\\Documents\\shared-svn\\projects\\realLabHH\\data\\hamburg_shapeFile\\hamburg_metropo\\hamburg_metropo.shp";
        }

        try {
            incomeBasedSelection = Boolean.parseBoolean(args[16]);
            log.info("Using income based selection");
        }

        catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Not using income based selection");
            incomeBasedSelection = false;
        }

        try {
            shareOfAgents = Double.parseDouble(args[18]);
        }

        catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Not able to read  the share of Agents using default value");
            shareOfAgents = 0.1;
        }
    }
}

