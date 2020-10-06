package org.matsim.hamburg.replanning.modules;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author zmeng
 */
public class HamburgSubtourModeChoicePlanStrategy implements PlanStrategy {
    private final PlanStrategy planStrategyDelegate;

    @Inject
    HamburgSubtourModeChoicePlanStrategy(final Scenario scenario, final Provider<TripRouter> tripRouterProvider) {

        HamburgSubtourModeChoiceStrategyFactory factory = new HamburgSubtourModeChoiceStrategyFactory(scenario, tripRouterProvider);
        this.planStrategyDelegate = factory.get();
    }

    @Override
    public void finish() {
        this.planStrategyDelegate.finish();
    }

    @Override
    public void init(ReplanningContext replanningContext) {
        this.planStrategyDelegate.init(replanningContext);
    }

    @Override
    public void run(HasPlansAndId<Plan, Person> person) {
        this.planStrategyDelegate.run(person);
    }
}
