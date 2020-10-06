
package org.matsim.hamburg.replanning.modules;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author cdobler, senozon AG
 */
public class HamburgSubtourModeChoiceStrategyFactory implements Provider<PlanStrategy> {

	private final Scenario scenario;
	private final Provider<TripRouter> tripRouterProvider;

    @Inject
    protected HamburgSubtourModeChoiceStrategyFactory(final Scenario scenario, final Provider<TripRouter> tripRouterProvider) {
        this.scenario = scenario;
        this.tripRouterProvider = tripRouterProvider;
    }

    @Override
	public PlanStrategy get() {
    	PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>());
    	builder.addStrategyModule(new TripsToLegsModule(this.scenario.getConfig().global()));
    	builder.addStrategyModule(new HamburgSubtourModeChoice(this.scenario.getConfig()));
    	builder.addStrategyModule(new ReRoute(this.scenario, this.tripRouterProvider));
    	return builder.build();
	}
}
