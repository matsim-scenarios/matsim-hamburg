package org.matsim.core.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.*;

/**
 * @author zmeng
 */
public class IncomeBasedPlanScoringFunctionFactory implements ScoringFunctionFactory {

    private final ScoringParametersForPerson scoringParametersForPerson;
    private final Scenario scenario;

    public IncomeBasedPlanScoringFunctionFactory(final Scenario sc) {
        this.scenario = sc;
        this.scoringParametersForPerson = new SubpopulationScoringParameters(sc);

    }

    @Override
    public ScoringFunction createNewScoringFunction(Person person) {
        Config config = scenario.getConfig();

        // average income in metro-hamburg: see: http://www.statistik-nord.de/fileadmin/maps/Metropolregion/atlas.html
        double averageIncome = 23146;
        double personSpecificMarginalUtilityOfMoney = config.planCalcScore().getMarginalUtilityOfMoney();

        if(person.getAttributes().getAsMap().containsKey("income"))
            personSpecificMarginalUtilityOfMoney = personSpecificMarginalUtilityOfMoney *(
                    averageIncome  / Double.parseDouble(person.getAttributes().getAttribute("income").toString()));
        config.planCalcScore().setMarginalUtilityOfMoney(personSpecificMarginalUtilityOfMoney);

        ScoringParameters.Builder builder = new ScoringParameters.Builder(ScenarioUtils.loadScenario(config),person);
        ScoringParameters scoringParameters = builder.build();

        SumScoringFunction sumScoringFunction = new SumScoringFunction();
        sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(scoringParameters));
        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(scoringParameters, scenario.getNetwork(), this.scenario.getConfig().transit().getTransitModes()));
        sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(scoringParameters));
        sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(scoringParameters));

        return sumScoringFunction;

    }
}
