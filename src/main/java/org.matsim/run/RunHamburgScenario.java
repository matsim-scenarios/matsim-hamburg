package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import picocli.CommandLine;

import java.util.List;

/**
 * @author zmeng
 */
@CommandLine.Command(
        header = ":: Open Hamburg Scenario ::",
        version = RunHamburgScenario.VERSION
)
public class RunHamburgScenario extends MATSimApplication{
    public static final String COORDINATE_SYSTEM = "EPSG:25832";
    public static final String VERSION = "v1.0";
    public static final int SCALE = 1;

    @CommandLine.Option(names = {"--run-id"}, defaultValue = "hamburg"+VERSION+"-"+SCALE+"pct", description = "run id")
    private String runId;
    @CommandLine.Option(names = {"--output-directory"}, defaultValue = "scenarios/output/", description = "output directory")
    private String outputDirectory;

    public RunHamburgScenario() {
        super(String.format("scenarios/input/hamburg-%s-%dpct.config.xml", VERSION, SCALE));
    }

    public static void main(String[] args) {
        MATSimApplication.run(RunHamburgScenario.class, args);
    }

    @Override
    protected Config prepareConfig(Config config) {
       config.controler().setRunId(runId);
       config.controler().setOutputDirectory(outputDirectory);

        for (long ii = 600; ii <= 97200; ii += 600) {

            for (String act : List.of("home", "restaurant", "other", "visit", "errands", "educ_higher", "educ_secondary")) {
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii + ".0").setTypicalDuration(ii));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shopping_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
        }

       return config;
    }

    @Override
    protected List<ConfigGroup> getCustomModules() {
        return super.getCustomModules();
    }

    @Override
    protected void prepareControler(Controler controler) {
        super.prepareControler(controler);
    }

    @Override
    protected void prepareScenario(Scenario scenario) {
        super.prepareScenario(scenario);
    }
}

