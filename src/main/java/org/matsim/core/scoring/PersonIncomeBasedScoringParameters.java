package org.matsim.core.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tschlenther
 * coped from https://github.com/matsim-scenarios/matsim-berlin/tree/personSpecificUtilityOfMoney
 */
public class PersonIncomeBasedScoringParameters implements ScoringParametersForPerson {
    Logger log = Logger.getLogger(PersonIncomeBasedScoringParameters.class);

    public static final String INCOME_ATTRIBUTE_NAME = "income";

    private final PlanCalcScoreConfigGroup config;
    private final ScenarioConfigGroup scConfig;
    private final TransitConfigGroup transitConfigGroup;
    private final Map<Id<Person>, ScoringParameters> params = new HashMap<>();
    private final double globalAvgIncome;

    @Inject
    PersonIncomeBasedScoringParameters(Population population, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ScenarioConfigGroup scenarioConfigGroup, TransitConfigGroup transitConfigGroup) {
        this.config = planCalcScoreConfigGroup;
        this.scConfig = scenarioConfigGroup;
        this.transitConfigGroup = transitConfigGroup;
        this.globalAvgIncome = computeAvgIncome(population);
    }

    private double computeAvgIncome(Population population) {
        log.info("reading income attribute '" + INCOME_ATTRIBUTE_NAME + "' of all agents in 'person' subpopulation...");
        return population.getPersons().values().stream()
                .filter(person -> PopulationUtils.getSubpopulation(person).equals("person")) //consider true persons only
                .mapToDouble(person -> {
                    if(person.getAttributes().getAttribute(INCOME_ATTRIBUTE_NAME) == null)
                        throw new IllegalStateException("you are trying to use person specific marginal utilitiess of money but" +
                                " person " + person + "has no " + INCOME_ATTRIBUTE_NAME + " attribute!");
                    return (double) person.getAttributes().getAttribute(INCOME_ATTRIBUTE_NAME);
                })
                .average().getAsDouble();
    }

//	public OpenBerlinPersonScoringParameters(Scenario scenario) {
//		this(scenario.getConfig().plans(), scenario.getConfig().planCalcScore(), scenario.getConfig().scenario(), scenario.getPopulation(), scenario.getConfig().transit());
//	}

    @Override
    public ScoringParameters getScoringParameters(Person person) {

        if (!this.params.containsKey(person.getId())) {
            final String subpopulation = PopulationUtils.getSubpopulation( person );
            /* lazy initialization of params. not strictly thread safe, as different threads could
             * end up with different params-object, although all objects will have the same
             * values in them due to using the same config. Still much better from a memory performance
             * point of view than giving each ScoringFunction its own copy of the params.
             */
            ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config, this.config.getScoringParameters(subpopulation), scConfig);
            if (transitConfigGroup.isUseTransit()) {
                // yyyy this should go away somehow. :-)



                PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
                transitActivityParams.setTypicalDuration(120.0);
                transitActivityParams.setOpeningTime(0.) ;
                transitActivityParams.setClosingTime(0.) ;
                ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
                modeParamsBuilder.setScoreAtAll(false);
                builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder);
            }

            if (subpopulation.equals("person")){
                //this is where we put person-specific stuff
                builder.setMarginalUtilityOfMoney( globalAvgIncome / (double) person.getAttributes().getAttribute(INCOME_ATTRIBUTE_NAME));
            }

            this.params.put(
                    person.getId(),
                    builder.build());
        }

        return this.params.get(person.getId());
    }
}

