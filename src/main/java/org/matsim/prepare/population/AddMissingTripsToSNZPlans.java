package org.matsim.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zmeng
 */
public class AddMissingTripsToSNZPlans {
    private static final Logger log = LogManager.getLogger(AddMissingTripsToSNZPlans.class);
    private static final Random rnd = MatsimRandom.getLocalInstance();
    private static int addingTrips = 0;
    private static double range;

    private Population population;
    private int numOfMissingTrips;
    private double rangeForShortDistanceTrips;
    private double maxShortRangeActivityDuration = 3600.;

    public static void main(String[] args) {
        String plans;
        String personInHamburg;
        String outputFolder;
        int missingTrips;

        if(args.length == 0){
            plans = "test/input/test-hamburg.plans.xml";
            personInHamburg = "../shared-svn/projects/matsim-hamburg/hamburg-v1.0/person_specific_info/person_in_hamburg.csv";
            outputFolder = "test/output/";
            missingTrips = 10;
            range = 1000;
        } else {
            plans = args[0];
            personInHamburg = args[1];
            outputFolder = args[2];
            missingTrips = Integer.parseInt(args[3]);
            range = Integer.parseInt(args[4]);
        }

        List<String> personInHam = new ArrayList<>();
        File file = new File(personInHamburg);
        try {
            BufferedReader csvReader = new BufferedReader(new FileReader(file));
            String firstLine = csvReader.readLine();
            while ((firstLine = csvReader.readLine()) != null) {
                personInHam.add(firstLine);
            }
            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



        Predicate<String> condition = personInHam::contains;
//        Predicate<String> condition = new Predicate<String>() {
//            @Override
//            public boolean test(String s) {
//                return true;
//            }
//        };

        AddMissingTripsToSNZPlans addMissingTripsToSNZPlans = new AddMissingTripsToSNZPlans(plans, missingTrips, range);
        addMissingTripsToSNZPlans.run(condition);
        PopulationUtils.writePopulation(addMissingTripsToSNZPlans.getPopulation(), outputFolder + "hamburg-add-"+missingTrips+"-trips.plans.xml.gz");

        long totalTrips = addMissingTripsToSNZPlans.population.getPersons().values().stream().filter(person -> condition.test(person.getId().toString()))
                .map(HasPlansAndId::getSelectedPlan)
                .map(plan -> TripStructureUtils.getTrips(plan.getPlanElements()))
                .mapToLong(List::size)
                .sum();
        log.info("trip num after updating: " +totalTrips);
    }

    public AddMissingTripsToSNZPlans(String snz_population, int numOfMissingTrips, double rangeForShortDistanceTrips) {

        Config config = ConfigUtils.createConfig();

        config.plans().setInputFile(snz_population);

        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputCRS("EPSG:25832");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        this.population = scenario.getPopulation();
        this.numOfMissingTrips = numOfMissingTrips;
        this.rangeForShortDistanceTrips = rangeForShortDistanceTrips;
    }

    public void run(Predicate<String> addingCondition) {

        double probability = computeAddingProbability(numOfMissingTrips, addingCondition);
        log.info("probability of adding trips is: " + probability);
        log.info("adding missing trips.........."+addingTrips);
        for (Person person :
                population.getPersons().values()) {
            if (addingCondition.test(person.getId().toString())) {

                Plan plan = person.getSelectedPlan();
                var activities = TripStructureUtils.getActivities(plan.getPlanElements(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                var filterActivities = activityFilter(activities);
                List<Activity> markedActivities = new ArrayList<>();

                markedActivities = filterActivities.stream().filter(x -> rnd.nextDouble() < probability).collect(Collectors.toList());

                if (markedActivities.size() > 0) {
                    Plan newPlan = population.getFactory().createPlan();
                    RoutingModeMainModeIdentifier mainModeIdentifier = new RoutingModeMainModeIdentifier();
                    Activity lastActivity = null;
                    for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan.getPlanElements())) {
                        Activity originActivity = trip.getOriginActivity();
                        if (markedActivities.contains(originActivity)) {

                            addingTrips+=2;

                            if(addingTrips%100 == 0){
                                log.info("adding missing trips.........."+addingTrips);
                            }
                            // add activity

                            double range = rnd.nextDouble() * rangeForShortDistanceTrips;
                            double walkTime = range / 1.2 * 2;
                            double maxDurationForShortDistanceTrips = Math.max(originActivity.getEndTime().seconds() - originActivity.getStartTime().seconds() - walkTime, 1);
                            double duration = Math.min(this.maxShortRangeActivityDuration,rnd.nextDouble() * maxDurationForShortDistanceTrips);
                            double newEndTime = updateEndTime(originActivity, duration, walkTime);


                            Activity activity1 = population.getFactory().createActivityFromCoord(originActivity.getType(), originActivity.getCoord());
                            activity1.setLinkId(originActivity.getLinkId());
                            activity1.setStartTime(originActivity.getStartTime().seconds());
                            activity1.setEndTime(newEndTime);
                            newPlan.addActivity(activity1);

                            Leg leg1 = population.getFactory().createLeg(TransportMode.walk);
                            newPlan.addLeg(leg1);


                            Activity shortDistanceRangeActivity = population.getFactory().createActivityFromCoord("other", getShortDistanceCoordinate(trip.getOriginActivity().getCoord(), range));
                            shortDistanceRangeActivity.setMaximumDuration(duration);
                            shortDistanceRangeActivity.setStartTime(newEndTime + walkTime/2);
                            shortDistanceRangeActivity.setEndTime(newEndTime + walkTime/2 + duration);
                            newPlan.addActivity(shortDistanceRangeActivity);

                            Leg leg2 = population.getFactory().createLeg(TransportMode.walk);
                            newPlan.addLeg(leg2);

                            Activity activity2 = population.getFactory().createActivityFromCoord(originActivity.getType(), originActivity.getCoord());
                            activity2.setLinkId(originActivity.getLinkId());
                            activity2.setStartTime(updateStartTime(originActivity, shortDistanceRangeActivity.getEndTime().seconds() + walkTime/2));
                            activity2.setEndTime(originActivity.getEndTime().seconds());
                            newPlan.addActivity(activity2);

                            String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                            Leg leg = population.getFactory().createLeg(mainMode);
                            newPlan.addLeg(leg);

                        } else {
                            newPlan.addActivity(originActivity);
                            String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                            Leg leg = population.getFactory().createLeg(mainMode);
                            newPlan.addLeg(leg);
                        }
                        lastActivity = trip.getDestinationActivity();
                    }
                    newPlan.addActivity(lastActivity);
                    person.removePlan(plan);
                    person.addPlan(newPlan);
                }
            }
        }
        log.info("adding missing trips.........."+addingTrips + "..finished");
    }


    private Coord getShortDistanceCoordinate(Coord coord, double range) {
        final double f = Math.sqrt(2) / 2;
        Coord newCoord = new Coord(coord.getX() + f * range, coord.getY() + f * range);
        return newCoord;
    }

    private double updateEndTime(Activity originActivity, double duration, double v) {
        double actDuration = originActivity.getEndTime().seconds() - originActivity.getStartTime().seconds();
        double longestDuration = Math.max(actDuration - duration - v, 0);
        return rnd.nextDouble() * longestDuration + originActivity.getStartTime().seconds();
    }

    private double updateStartTime(Activity originActivity, double v) {
        return Math.min(originActivity.getEndTime().seconds(), v);
    }

    private double computeAddingProbability(int numOfMissingTrips, Predicate<String> addingCondition) {
        int numOfAct = 0;
        int numOfTrips = 0;
        for (Person person :
                population.getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            person.getPlans().clear();
            person.addPlan(selectedPlan);
            person.setSelectedPlan(selectedPlan);

            if (addingCondition.test(person.getId().toString())){
                var activities = TripStructureUtils.getActivities(selectedPlan.getPlanElements(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                var trips = TripStructureUtils.getTrips(selectedPlan);
                numOfAct += activityFilter(activities).size();
                numOfTrips += trips.size();
            }
        }
        log.info("activities: "+numOfAct+", trips: "+numOfTrips+ ", missing trips: "+numOfMissingTrips);
        return (double)(numOfMissingTrips)/2/numOfAct;
    }

    private List<Activity> activityFilter (List<Activity> activities){
        // the first, last, and too short activities can not be split to add other activity
        var filterAct = new LinkedList<Activity>();
        if(activities.size() > 2){
            for (int i = 1; i < activities.size()-2; i++) {
                double startTime = activities.get(i).getStartTime().seconds();
                double endTime = activities.get(i).getEndTime().seconds();
                if((endTime-startTime) > range * 2 * 1.5)
                    filterAct.add(activities.get(i));
            }
        }
        return filterAct;
    }

    public Population getPopulation() {
        return population;
    }
}
