package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.matsim.run.RunBaseCaseHamburgScenario.VERSION;
/**
 * @author zmeng
 */
public class PreparePopulation {

    Scenario scenario;
    Path output;

    public PreparePopulation(String initialDemand,String attributes, Path output) {

        Config config = ConfigUtils.createConfig();

        config.plans().setInputPersonAttributeFile(attributes);
        config.plans().setInputFile(initialDemand);

        config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);

        config.global().setCoordinateSystem("EPSG:25832");
        config.plans().setInputCRS("EPSG:25832");

        scenario = ScenarioUtils.loadScenario(config);

        this.output = output;
    }

    public static void main(String[] args) throws IOException {

        // population files can not be public, thus they are stored privately in svn, to get the access of those folders please contact us in github

        String initialDemand = "../shared-svn/projects/realLabHH/svn/realLabHH/matsim-input-files/v1/initialDemand/optimizedPopulation.xml.gz";
        String attributes = "../shared-svn/projects/realLabHH/svn/realLabHH/matsim-input-files/v1/initialDemand/additionalPersonAttributes.xml.gz";
        String outputPath = "../shared-svn/project/realLabHH/svn/realLabHH/matsim-input-files/v1/";

        PreparePopulation preparePopulation = new PreparePopulation(initialDemand, attributes, Path.of(outputPath));
        preparePopulation.run();
    }

    public void run() throws IOException {
        for (Person person :
             scenario.getPopulation().getPersons().values()) {

            person.getAttributes().putAttribute("subpopulation", "person");

            if(Integer.parseInt(person.getAttributes().getAttribute("age").toString()) < 17)
                PersonUtils.setCarAvail(person, "never");
            else
                PersonUtils.setCarAvail(person, person.getAttributes().getAttribute("sim_carAvailability").toString());

            for (Plan plan :
                    person.getPlans()) {
                Plan newPlan = preparePlan(plan);
                person.removePlan(plan);
                person.addPlan(newPlan);
            }
        }

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
