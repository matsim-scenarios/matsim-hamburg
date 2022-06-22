package org.matsim.run;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.population.ExtractHomeCoordinates;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;


/**
 * Run class using {@link MATSimApplication}
 *
 * @author rakow
 */
@MATSimApplication.Prepare({
		ExtractHomeCoordinates.class
})
@CommandLine.Command(header = ":: Open Hamburg Scenario ::", version = RunBaseCaseHamburgScenario.VERSION)
public class RunHamburgScenario extends MATSimApplication {

	private static final Logger log = Logger.getLogger(RunHamburgScenario.class);

	public static void main(String[] args) {
		MATSimApplication.run(RunHamburgScenario.class, args);
	}

	public RunHamburgScenario() {
		super("public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/baseCase/hamburg-v3.0-25pct.config.baseCase.xml");
	}

	public RunHamburgScenario(@Nullable Config config) {
		super(config);
	}

	@Override
	protected Config prepareConfig(Config config) {

		RunBaseCaseHamburgScenario.prepareConfig(config);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		try {
			RunBaseCaseHamburgScenario.prepareScenario(scenario);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	@Override
	protected void prepareControler(Controler controler) {

		RunBaseCaseHamburgScenario.prepareControler(controler);

	}
}
