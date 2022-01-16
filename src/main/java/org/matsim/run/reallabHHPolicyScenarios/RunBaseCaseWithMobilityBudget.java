package org.matsim.run.reallabHHPolicyScenarios;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.prepare.SelectionMobilityBudget;
import org.matsim.run.HamburgExperimentalConfigGroup;
import org.matsim.run.RunBaseCaseHamburgScenario;

import javax.annotation.Nullable;
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

    //public static final HashMap<Id<Person>, Double > personsWithMobilityBudget = new HashMap<>();
    public final Map<Id<Person>, Double> personsEligibleForMobilityBudget = new HashMap<>();
    private Double dailyMobilityBudget;
    private double shareOfIncome;
    private String shapeFile;
    private boolean incomeBasedSelection;
    private final double shareOfAgents;
    private boolean basedOnCarUse;

    /*i decided to move from static methods to an object-oriented approach as a temporary solution to handle the program arguments and all the field variables. The latter should basically be moved
     * into a config group (e.g. HamburgExperimentalConfigGroup
     */
    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        double dailyMobilityBudget = 10;
        Double shareOfIncome = 0.0;
        String shapeFile = null;
        boolean incomeBasedSelection = false;
        double shareOfAgents = 0.;
        boolean basedOnCarUse = false;

        if (args.length == 0) {
            args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.2/input/baseCase/hamburg-v2.2-10pct.config.baseCase.xml"};
        }

        RunBaseCaseWithMobilityBudget runner = new RunBaseCaseWithMobilityBudget(dailyMobilityBudget, shareOfIncome, shapeFile, incomeBasedSelection, shareOfAgents, basedOnCarUse);

        Config config = runner.prepareConfig(args);
        Scenario scenario = runner.prepareScenario(config);
        Controler controler = runner.prepareControler(scenario);
        controler.run();
        log.info("Done.");
    }


    //TODO move variables to config group or use a builder...
    // kind of had to to this as a quick, intermediate fix.., tschlenther late sep, '21

    /**
     *  @param dailyMobilityBudget
     * @param shareOfIncome set to 0.0 or negative in order to disable incomeBasedMobilityBudget
     * @param shapeFile
     * @param incomeBasedSelection
     * @param shareOfAgents
     * @param basedOnCarUse
     */
    /*package*/ RunBaseCaseWithMobilityBudget(Double dailyMobilityBudget,
                                              double shareOfIncome,
                                              @Nullable String shapeFile,
                                              boolean incomeBasedSelection,
                                              double shareOfAgents,
                                              boolean basedOnCarUse) {
        this.dailyMobilityBudget = dailyMobilityBudget;
        this.shareOfIncome = shareOfIncome;
        this.shapeFile = shapeFile;
        this.incomeBasedSelection = incomeBasedSelection;
        this.shareOfAgents = shareOfAgents;
        this.basedOnCarUse = basedOnCarUse;
    }


    Controler prepareControler(Scenario scenario) {
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

    Scenario prepareScenario(Config config) throws IOException {

        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        dailyMobilityBudget = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).getfixedDailyMobilityBudget();
        Preconditions.checkNotNull(dailyMobilityBudget, "you need to specify fixedDailyMobilityBudget in " + HamburgExperimentalConfigGroup.class);

        for (Map.Entry<Id<Person>, Double> entry : getPersonsEligibleForMobilityBudget2FixedValue(scenario, dailyMobilityBudget).entrySet()) {
            Id<Person> person = entry.getKey();
            Double budget = entry.getValue();
            personsEligibleForMobilityBudget.put(person, budget);
        }

        shareOfIncome = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).getShareOfIncome();
        log.info("using income " + (shareOfIncome > 0.0));
        log.info("share of income "+ shareOfIncome);
        if (shareOfIncome > 0.) {
            log.info("using the income for the MobilityBudget");
            for (Id<Person> personId : personsEligibleForMobilityBudget.keySet()) {
                double monthlyIncomeOfAgent = (double) scenario.getPopulation().getPersons().get(personId).getAttributes().getAttribute("income")/12;
                //divided by 30 because income is needed per day
                double incomeOfAgent = monthlyIncomeOfAgent/30;
                dailyMobilityBudget = incomeOfAgent * shareOfIncome;
                personsEligibleForMobilityBudget.replace(personId, dailyMobilityBudget);
            }
        }

        shapeFile = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).getShapeFile();
        if (shapeFile != null) {
            log.info("Filtering for Region " + shapeFile);
            SelectionMobilityBudget.filterForRegion(scenario.getPopulation(), shapeFile, personsEligibleForMobilityBudget );
        }

        incomeBasedSelection = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).isIncomeBasedSelection();
        if (incomeBasedSelection==true) {
            log.info("Selceting Agents based on Income " + incomeBasedSelection);
            SelectionMobilityBudget.incomeBasedSelection(scenario.getPopulation(),shareOfAgents, personsEligibleForMobilityBudget);
        }

        basedOnCarUse = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class).isBasedOnCarUse();
        if (basedOnCarUse==true) {
            log.info("Based on Car Use");
            SelectionMobilityBudget.basedOnCarUse(scenario.getPopulation(), scenario.getConfig(), personsEligibleForMobilityBudget);
        }

        return scenario;
    }

    static Map<Id<Person>, Double> getPersonsEligibleForMobilityBudget2FixedValue(Scenario scenario, Double value) {

        Map<Id<Person>, Double> persons2Budget = new HashMap<>();

        log.info("filtering population for mobilityBudget");

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
                   persons2Budget.put(personId, value);
                }
            }
        }
        return persons2Budget;
    }

     Config prepareConfig(String[] args, ConfigGroup... customModules) {
        log.info("Preparing config");
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModules);

        return config;
    }

}

