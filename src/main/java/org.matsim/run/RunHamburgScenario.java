package org.matsim.run;

import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
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
    public static final double[] X_EXTENT = new double[]{490826.5738238178, 647310.6279172485};
    public static final double[] Y_EXTENT = new double[]{5866434.167201331, 5996884.970634732};

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
       config.controler().setOutputDirectory(outputDirectory + "/" + runId);

        for (long ii = 600; ii <= 97200; ii += 600) {

            for (String act : List.of("educ_higher", "educ_tertiary", "educ_other", "home", "educ_primary", "errands", "educ_secondary", "visit")) {
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii + ".0").setTypicalDuration(ii));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("educ_kiga_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(18. * 3600.));
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
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                //todo which main mode identifier should we use
                bind(AnalysisMainModeIdentifier.class).to(DefaultAnalysisMainModeIdentifier.class);
            }
        });
    }

    @Override
    protected void prepareScenario(Scenario scenario) {
        super.prepareScenario(scenario);
    }
}

