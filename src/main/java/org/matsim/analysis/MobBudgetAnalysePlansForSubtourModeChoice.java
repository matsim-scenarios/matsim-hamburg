/*
package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.run.HamburgAnalysisMainModeIdentifier;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MobBudgetAnalysePlansForSubtourModeChoice {

    //private static final HamburgAnalysisMainModeIdentifier mainModeIdentifier = new HamburgAnalysisMainModeIdentifier();
    private static final Set<String> chainBasedModes = Set.of(TransportMode.bike, TransportMode.car);
    private static final Set<String> modes = Set.of("car","pt","bike","walk");


    public static void main(String[] args) throws IOException {

        Population population = PopulationUtils.readPopulation("D://Arbeit//shared-svn//projects//matsim-hamburg//hamburg-v2//hamburg-v2.0//input//hamburg-v2.0-10pct.plans.xml.gz");

        Map<Integer, Integer> nrOfSubtours2NrOfAgents = new HashMap<>();
        Map<Integer, Integer> nrOfProblematicSubtours2NrOfAgents = new HashMap<>();
        List<Id<Person>> listOfProblematicAgentsForMobBudget = new ArrayList<>();

        int nrOfCarAgents = 0;
        int nrOfCarAgentsNotResponsiveToMobilityBudget = 0;
        int nrOfAgentsWithFixedPlans = 0;
        int nrOfAgentsWithPartialProblematicPlan = 0;
        int nrOfAgentsWith0Trips = 0;

        HashSet<TripStructureUtils.Subtour> allSubtours = new HashSet<>();

        for(Person person :population.getPersons().values()){

            Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(person.getSelectedPlan());
            allSubtours.addAll(subtours);

            if(TripStructureUtils.getTrips(person.getSelectedPlan()).size() == 0) {
                nrOfAgentsWith0Trips++;
                nrOfSubtours2NrOfAgents.compute(subtours.size(), (k, v) -> v == null ? 1 : v + 1);
            } else {
                nrOfSubtours2NrOfAgents.compute(subtours.size(), (k, v) -> v == null ? 1 : v + 1);
            }

            long count = 0L;
            for (TripStructureUtils.Subtour subtour1 : subtours) {
                if (isProblematic(subtour1)) {
                    count++;
                }
            }
            int nrOfProblematicSubTours = (int) count;

            List<TripStructureUtils.Subtour> carSubtours = new ArrayList<>();
            for (TripStructureUtils.Subtour subtour : subtours) {
                if (hasCarLeg(subtour)) {
                    carSubtours.add(subtour);
                }
            }

            if(carSubtours.size() > 0){
                nrOfCarAgents++;


                for (TripStructureUtils.Subtour carSubtour : carSubtours) {
                    if (isProblematic(carSubtour)) {
                        nrOfCarAgentsNotResponsiveToMobilityBudget++;
                        listOfProblematicAgentsForMobBudget.add(person.getId());
                        break;
                    }
                }
            }

            if (nrOfProblematicSubTours == subtours.size()){
                nrOfAgentsWithFixedPlans ++;
            } else if( nrOfProblematicSubTours > 0 && nrOfProblematicSubTours < subtours.size()){
                nrOfAgentsWithPartialProblematicPlan ++;
            }
            nrOfProblematicSubtours2NrOfAgents.compute(nrOfProblematicSubTours, (k,v) -> v == null ? 1 : v + 1);

            writeList2CSV(listOfProblematicAgentsForMobBudget);

        }

        System.out.println("###################");
        System.out.println("### total number of agents: " + population.getPersons().size());
        System.out.println("###################");
        System.out.println("### number of agents with 0 trips: " + nrOfAgentsWith0Trips);
        System.out.println("###################");
        nrOfSubtours2NrOfAgents.forEach( (k,v) -> System.out.println("### " + k + " subtours:\t" + v + " agents"));
        System.out.println("###################");
        nrOfProblematicSubtours2NrOfAgents.forEach( (k,v) -> System.out.println("### " + k + " problematic subtours:\t" + v + " agents"));
        System.out.println("###################");
        System.out.println("### number of agents with completely problematic plans: " + nrOfAgentsWithFixedPlans + " = " + ((double) nrOfAgentsWithFixedPlans / ((double) population.getPersons().size()) + "%"));
        System.out.println("### number of agents with partially problematic plans: " + nrOfAgentsWithPartialProblematicPlan + " = " + ((double) nrOfAgentsWithPartialProblematicPlan / ((double) population.getPersons().size()) + "%"));
        System.out.println("###################");
        int mobileButFixed = nrOfAgentsWithFixedPlans - nrOfAgentsWith0Trips;
        System.out.println("### number of MOBILE agents with completely problematic plans: "
                + mobileButFixed + " = " + ((double) mobileButFixed / ((double) population.getPersons().size() - nrOfAgentsWith0Trips) + "%"));
        System.out.println("###################");
        System.out.println("### number of car agents " + nrOfCarAgents);
        System.out.println("### number of car agents that are not responsive to mobility budget: " + nrOfCarAgentsNotResponsiveToMobilityBudget + " = "
                + ((double) nrOfCarAgentsNotResponsiveToMobilityBudget / ((double) nrOfCarAgents) + "%"));
        System.out.println("###################");
        List<TripStructureUtils.Subtour> subToursWithAtLeastOneCarLeg = allSubtours.stream()
                .filter(MobBudgetAnalysePlansForSubtourModeChoice::hasCarLeg)
                .collect(Collectors.toList());

        List<TripStructureUtils.Subtour> problematicCarSubTours = new ArrayList<>();
        for (TripStructureUtils.Subtour subtour : subToursWithAtLeastOneCarLeg) {
            if (isProblematic(subtour)) {
                problematicCarSubTours.add(subtour);
            }
        }
        System.out.println("### number of subtours with at least one car leg: " + subToursWithAtLeastOneCarLeg.size());
        System.out.println("### number of subtours with at least one car leg that are problematic: " + problematicCarSubTours.size()
                + " = " + ((double) problematicCarSubTours.size() / (double) subToursWithAtLeastOneCarLeg.size() 	) + "%");

        System.out.println("######FINISHED#####");
    }

    private static boolean hasCarLeg(TripStructureUtils.Subtour subtour) {
        return subtour.getTrips().stream()
                .flatMap(trip -> trip.getLegsOnly().stream())
                .filter(leg -> leg.getMode().equals(TransportMode.car))
                .findAny()
                .isPresent();
    }

    private static boolean isUser(Person person, String mode) {
        return TripStructureUtils.getLegs(person.getSelectedPlan()).stream()
                .filter(leg -> leg.getMode().equals(mode))
                .findAny()
                .isPresent();
    }


    */
/**
     * in other words: will not be mutated by ChoosRandomeLegModeForSubtour
     * @param subtour
     * @return
     *//*

    private static boolean isProblematic(TripStructureUtils.Subtour subtour) {
        return !subtour.isClosed() || !isMassConserving(subtour) || containsUnknownMode(subtour);
    }


    ///-----------------------copied from ChooseRandomLegModeForSubtour

    private static boolean containsUnknownMode(final TripStructureUtils.Subtour subtour) {
        for (TripStructureUtils.Trip trip : subtour.getTrips()) {
            if (!modes.contains( mainModeIdentifier.identifyMainMode( trip.getTripElements() ))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMassConserving(final TripStructureUtils.Subtour subtour) {
        for (String mode : chainBasedModes) {
            if (!isMassConserving(subtour, mode)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMassConserving(
            final TripStructureUtils.Subtour subtour,
            final String mode) {
        final Activity firstOrigin =
                findFirstOriginOfMode(
                        subtour.getTrips(),
                        mode);

        if (firstOrigin == null) {
            return true;
        }

        final Activity lastDestination =
                findLastDestinationOfMode(
                        subtour.getTrips(),
                        mode);

        return atSameLocation(firstOrigin, lastDestination);
    }


    private static boolean atSameLocation(Activity firstLegUsingMode,
                                          Activity lastLegUsingMode) {
        return firstLegUsingMode.getFacilityId()!=null ?
                firstLegUsingMode.getFacilityId().equals(
                        lastLegUsingMode.getFacilityId() ) :
                firstLegUsingMode.getLinkId().equals(
                        lastLegUsingMode.getLinkId() );
    }

    private static Activity findLastDestinationOfMode(
            final List<TripStructureUtils.Trip> tripsToSearch,
            final String mode) {
        final List<TripStructureUtils.Trip> reversed = new ArrayList<>(tripsToSearch);
        Collections.reverse( reversed );
        for (TripStructureUtils.Trip trip : reversed) {
            if ( mode.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
                return trip.getDestinationActivity();
            }
        }
        return null;
    }

    private static Activity findFirstOriginOfMode(
            final List<TripStructureUtils.Trip> tripsToSearch,
            final String mode) {
        for (TripStructureUtils.Trip trip : tripsToSearch) {
            if ( mode.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
                return trip.getOriginActivity();
            }
        }
        return null;
    }

    private static void writeList2CSV (List originalList) throws IOException {
        FileWriter writer = new FileWriter("personIdElegibleForMobBud.csv");
        writer.write("personId");
        writer.append("\n");

        for (int i = 0; i < originalList.size(); i++) {
            writer.append((String) originalList.get(i).toString());
            writer.append("\n");
        }
        writer.close();
    }


}
*/
