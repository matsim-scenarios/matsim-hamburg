package org.matsim.parking;

import com.google.inject.Provides;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author zmeng
 */
public class VehicleHandlerForParkingTest{

    @Test
    public void testVehicleHandler() {
        double result1 = this.runTestScenario(0);
        double result2 = this.runTestScenario(1000);
        Assert.assertEquals("differ between result1 and result2 should be 1000, not " + (result2 - result1),1000, result2-result1, 1);
    }

    public double runTestScenario(double parkTime) {
        Scenario scenario = this.createScenario();

        scenario.getNetwork().getLinks().values().forEach(link -> link.getAttributes().putAttribute("parkTime",parkTime));
        Controler controler = new Controler(scenario);

        controler.addOverridingQSimModule(new AbstractQSimModule() {

            protected void configureQSim() {
            }
            @Provides
            QNetworkFactory provideQNetworkFactory(EventsManager eventsManager, Scenario scenario, QSim qSim) {
                VehicleHandlerForParking vehicleHandlerForParking;
                ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(eventsManager, scenario);
                factory.setVehicleHandler(new VehicleHandlerForParking(qSim));
                return factory;
            }
        });
        controler.run();

        ExperiencedPlansService experiencedPlansService = controler.getInjector().getInstance(ExperiencedPlansService.class);
        double avg = experiencedPlansService.getExperiencedPlans().values().stream().map(TripStructureUtils::getLegs).flatMap(legs -> legs.stream())
                .map(Leg::getTravelTime).mapToDouble(OptionalTime::seconds).average().getAsDouble();

        return avg;
    }

    private class PersonDepartureAndArrivalEventHandler implements PersonArrivalEventHandler, PersonDepartureEventHandler{
        @Override
        public void handleEvent(PersonDepartureEvent personDepartureEvent) {

        }
        @Override
        public void handleEvent(PersonArrivalEvent personArrivalEvent) {

        }
    }

    private Scenario createScenario() {
        Config config = ConfigUtils.createConfig();
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(0);
        PlanCalcScoreConfigGroup.ActivityParams genericParams = new PlanCalcScoreConfigGroup.ActivityParams("generic");
        genericParams.setTypicalDuration(1.0D);
        config.planCalcScore().addActivityParams(genericParams);
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        NetworkFactory networkFactory = network.getFactory();
        Node nodeA = networkFactory.createNode(Id.createNodeId("A"), new Coord(0.0D, 0.0D));
        Node nodeB = networkFactory.createNode(Id.createNodeId("B"), new Coord(1000.0D, 2000.0D));
        Node nodeC = networkFactory.createNode(Id.createNodeId("C"), new Coord(1000.0D, 3000.0D));
        Node nodeD = networkFactory.createNode(Id.createNodeId("D"), new Coord(1000.0D, 4000.0D));
        Node nodeE = networkFactory.createNode(Id.createNodeId("E"), new Coord(1000.0D, 5000.0D));
        Link linkAB = networkFactory.createLink(Id.createLinkId("AB"), nodeA, nodeB);
        Link linkBC = networkFactory.createLink(Id.createLinkId("BC"), nodeB, nodeC);
        Link linkCD = networkFactory.createLink(Id.createLinkId("CD"), nodeC, nodeD);
        Link linkDE = networkFactory.createLink(Id.createLinkId("DE"), nodeD, nodeE);
        List var10000 = Arrays.asList(nodeA, nodeB, nodeC, nodeD, nodeE);
        Objects.requireNonNull(network);
        var10000.forEach(node -> {
            network.addNode((Node) node);
        });
        var10000 = Arrays.asList(linkAB, linkBC, linkCD, linkDE);
        Objects.requireNonNull(network);
        var10000.forEach(link -> {
            network.addLink((Link) link);
        });
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();
        Person person1 = populationFactory.createPerson(Id.createPersonId("person1"));
        Person person2 = populationFactory.createPerson(Id.createPersonId("person2"));
        Person person3 = populationFactory.createPerson(Id.createPersonId("person3"));
        Iterator var20 = Arrays.asList(person1, person2, person3).iterator();

        while(var20.hasNext()) {
            Person person = (Person)var20.next();
            population.addPerson(person);
            Plan plan = populationFactory.createPlan();
            person.addPlan(plan);
            Activity activity = populationFactory.createActivityFromLinkId("generic", linkAB.getId());
            activity.setEndTime(0.0D);
            plan.addActivity(activity);
            Leg leg = populationFactory.createLeg("car");
            plan.addLeg(leg);
            activity = populationFactory.createActivityFromLinkId("generic", linkCD.getId());
            activity.setMaximumDuration(10000.0D);
            plan.addActivity(activity);
            leg = populationFactory.createLeg("car");
            plan.addLeg(leg);
            activity = populationFactory.createActivityFromLinkId("generic", linkDE.getId());
            plan.addActivity(activity);
        }

        return scenario;
    }
}