package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.calibration.InitialDemandCalibration;
import org.matsim.core.population.PopulationUtils;
import org.matsim.run.RunHamburgScenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.matsim.run.RunHamburgScenario.VERSION;

/**
 * @author zmeng
 */
public class PreparePopulation {
    String initialDemand;
    Scenario scenario;
    Path output;

    public PreparePopulation(String initialDemand,Path output) {
        this.initialDemand = initialDemand;
        InitialDemandCalibration initialDemandCalibration = new InitialDemandCalibration(initialDemand,null);
        this.scenario = initialDemandCalibration.getScenario();
        this.output = output;
    }

    public static void main(String[] args) throws IOException {
        String initialDemand = "/Users/meng/work/realLabHH/svn/realLabHH/matsim-input-files/v0_temporary-version/optimizedPopulation.xml.gz";
        String outputPath = "scenarios/input";

        PreparePopulation preparePopulation = new PreparePopulation(initialDemand, Path.of(outputPath));
        preparePopulation.run();
    }

    public void run() throws IOException {
        for (Person person :
             scenario.getPopulation().getPersons().values()) {

            person.getAttributes().putAttribute("subpopulation", "person");

            for (Plan plan :
                    person.getPlans()) {
                Plan newPlan = preparePlan(plan);
                person.removePlan(plan);
                person.addPlan(newPlan);
            }
        }
       // scenario.getPopulation().getAttributes().putAttribute("coordinateReferenceSystem", RunHamburgScenario.COORDINATE_SYSTEM);
        Files.createDirectories(output);
        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-25pct.plans.xml.gz").toString());

        // sample 25% to 10%
        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.4);
        org.matsim.core.population.PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-10pct.plans.xml.gz").toString());

        // sample 10% to 1%
        org.matsim.core.population.PopulationUtils.sampleDown(scenario.getPopulation(), 0.1);
        PopulationUtils.writePopulation(scenario.getPopulation(), output.resolve("hamburg-" + VERSION + "-1pct.plans.xml.gz").toString());

    }

    private Plan preparePlan(Plan plan) {

        Plan newPlan = scenario.getPopulation().getFactory().createPlan();
        List<PlanElement> planElements = plan.getPlanElements();

        for (PlanElement pe :
                planElements) {
            if (pe instanceof Activity) {
                Activity activity = (Activity)pe;
                activity.setFacilityId(null);
                activity.setLinkId(null);

                if (!activity.getType().contains("interaction"))
                    splitActivityTypesBasedOnDuration(activity);

                newPlan.addActivity(activity);

            } else if (pe instanceof Leg) {
                Leg leg = (Leg) pe;

                if(leg.getMode().contains("teleported")){
                    leg.setMode(leg.getMode().split("ed_")[1]);
                    leg.getAttributes().putAttribute("routingMode",leg.getMode());
                }

                leg.setRoute(null);
                newPlan.addLeg(leg);
            }
        }
        mergeOvernightActivities(newPlan);
        return newPlan;
    }

    /**
     * Split activities into typical durations to improve value of travel time savings calculation.
     *
     * @see playground.vsp.openberlinscenario.planmodification.CemdapPopulationTools
     */
    private void splitActivityTypesBasedOnDuration(Activity act) {

        final double timeBinSize_s = 600.;


        double duration = act.getEndTime().orElse(24 * 3600)
                - act.getStartTime().orElse(0);

        int durationCategoryNr = (int) Math.round((duration / timeBinSize_s));

        if (durationCategoryNr <= 0) {
            durationCategoryNr = 1;
        }

        String newType = act.getType() + "_" + (durationCategoryNr * timeBinSize_s);
        act.setType(newType);

    }

    /**
     * See {@link playground.vsp.openberlinscenario.planmodification.CemdapPopulationTools}.
     */
    private void mergeOvernightActivities(Plan plan) {

        if (plan.getPlanElements().size() > 1) {
            Activity firstActivity = (Activity) plan.getPlanElements().get(0);
            Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

            String firstBaseActivity = firstActivity.getType().split("_")[0];
            String lastBaseActivity = lastActivity.getType().split("_")[0];

            if (firstBaseActivity.equals(lastBaseActivity)) {
                double mergedDuration = Double.parseDouble(firstActivity.getType().split("_")[1]) + Double.parseDouble(lastActivity.getType().split("_")[1]);


                firstActivity.setType(firstBaseActivity + "_" + mergedDuration);
                lastActivity.setType(lastBaseActivity + "_" + mergedDuration);
            }
        }  // skipping plans with just one activity

    }
}
