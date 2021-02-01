package org.matsim.run;

import java.util.OptionalDouble;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoreEventScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

/**
 * A factory to create scoring functions with person-specific marginal utility of money.
 * Modified version based on 'CharyparNagelScoringFunctionFactory'.
 * 
 * @author ikaddoura, tschlenther
 */
public class HamburgPlanScoringFunctionFactory implements ScoringFunctionFactory {
	private static final Logger log = Logger.getLogger(HamburgPlanScoringFunctionFactory.class );

	private double globalAverageIncome = -1.0;

	private final Config config;
	private Network network;
	private Population population;

	private final ScoringParametersForPerson params;
	private int warnCnt = 0;
	
	public static final String PERSONAL_INCOME_ATTRIBUTE_NAME = "income";

	public HamburgPlanScoringFunctionFactory( final Scenario sc ) {
		this( sc.getConfig(), new SubpopulationScoringParameters( sc ) , sc.getNetwork() , sc.getPopulation());
	}

	@Inject
	HamburgPlanScoringFunctionFactory(Config config, ScoringParametersForPerson params, Network network, Population population) {
		this.config = config;
		this.params = params;
		this.network = network;
		this.population = population;
	}

	/**
     *
     * In every iteration, the framework creates a new ScoringFunction for each Person.
     * A ScoringFunction is much like an EventHandler: It reacts to scoring-relevant events
     * by accumulating them. After the iteration, it is asked for a score value.
     *
     * Since the factory method gets the Person, it can create a ScoringFunction
     * which depends on Person attributes. This implementation does not.
     *
	 * <li>The fact that you have a person-specific scoring function does not mean that the "creative" modules
	 * (such as route choice) are person-specific.  This is not a bug but a deliberate design concept in order 
	 * to reduce the consistency burden.  Instead, the creative modules should generate a diversity of possible
	 * solutions.  In order to do a better job, they may (or may not) use person-specific info.  kai, apr'11
	 * </ul>
	 * 
	 * @param person
	 * @return new ScoringFunction
	 */
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		if (this.globalAverageIncome <= 0.) {
			this.globalAverageIncome = computeAvgIncome(population);
		}
		
		double personIncome = globalAverageIncome;
		
		if (person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME) != null) {
			personIncome = (double) person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME); 
			
		} else {
			if (warnCnt <= 5) log.warn(person.getId().toString() + " does not have an income attribute. "
					+ "Will use the average annual income, thus the marginal utility of money will be 1.0");
			if (warnCnt == 5) log.warn("Further warnings will not be printed.");
			warnCnt++;
		}

		final String subpopulation = PopulationUtils.getSubpopulation( person );

		double personSpecificMarginalUtilityOfMoney = this.config.planCalcScore().getScoringParameters(subpopulation).getMarginalUtilityOfMoney() * globalAverageIncome  / personIncome ;
		person.getAttributes().putAttribute("marginalUtilityOfMoney", personSpecificMarginalUtilityOfMoney);

		final ScoringParameters parameters = params.getScoringParameters( person );

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters, personSpecificMarginalUtilityOfMoney, this.network, config.transit().getTransitModes() ));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( personSpecificMarginalUtilityOfMoney ));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;
	}
	
	private double computeAvgIncome(Population population) {
		log.info("read income attribute '" + PERSONAL_INCOME_ATTRIBUTE_NAME + "' of all agents and compute global average.\n" +
				"Make sure to set this attribute only to appropriate agents (i.e. true 'persons' and not freight agents) \n" +
				"Income values <= 0 are ignored. Agents that have negative or 0 income will use the marginalUtilityOfMOney in their subpopulation's scoring params..");
		OptionalDouble averageIncome =  population.getPersons().values().stream()
				.filter(person -> person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME) != null) //consider only agents that have a specific income provided
				.mapToDouble(person -> (double) person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME))
				.filter(dd -> dd > 0)
				.average();

		if(! averageIncome.isPresent()){
			throw new RuntimeException("you are using " + this.getClass() + " but there is not a single income attribute in the population! " +
					"If you are not aiming for person-specific marginalUtilityOfMOney, better use other PersonScoringParams, e.g. SUbpopulationPersonScoringParams, which have higher performance." +
					"Otherwise, please provide income attributes in the population...");
		} else {
			log.info("global average income is " + averageIncome);
			return averageIncome.getAsDouble();
		}
	}
	
}
