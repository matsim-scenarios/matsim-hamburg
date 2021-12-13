package org.matsim.prepare.freight.bvm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.matsim.prepare.freight.bvm.CreatFreightAgents.COMMERCIAL;

/**
 * @author zmeng
 */
public class AdjustScenarioForFreight {

    private static final Logger log = Logger.getLogger(AdjustScenarioForFreight.class);
    private static final List<String> modes = Arrays.asList("Lfw","Lkw-g","Lkw-k","Lkw-m","Trans","PWV_IV","Pkw-Lfw");

    @Deprecated
    public static void adjustScenarioForFreight(Scenario scenario, List<String> modes){
        // network
        Network network = scenario.getNetwork();
        for (Link link : network.getLinks().values()) {
            if(link.getAllowedModes().contains("car")){
                var allowedModes = link.getAllowedModes();
                HashSet<String> newAllowedModes = getModesWithFreight(modes, allowedModes);
                link.setAllowedModes(newAllowedModes);
            }
        }

        // Config
        Config config = scenario.getConfig();
        config.plansCalcRoute().setNetworkModes(getModesWithFreight(modes,config.plansCalcRoute().getNetworkModes()));
        config.qsim().setMainModes(getModesWithFreight(modes,config.qsim().getMainModes()));

        config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setSubpopulation(COMMERCIAL).setWeight(0.95));
        config.strategy().addStrategySettings(new StrategyConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setSubpopulation(COMMERCIAL).setWeight(0.05));

        config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(COMMERCIAL).setTypicalDuration(12*3600.));

        for (String mode : modes) {
            config.planCalcScore().addModeParams(new PlanCalcScoreConfigGroup.ModeParams(mode).setMonetaryDistanceRate(-0.0004));
        }

        log.info("will delete routes from commercial legs and set coords of commercial activities to coord of their link!! This requires that the input plans were routed at least once, beforehand!");
        scenario.getPopulation().getPersons().values().stream()
                .filter(person -> PopulationUtils.getSubpopulation(person).equals(COMMERCIAL))
                .flatMap(person -> person.getSelectedPlan().getPlanElements().stream())
                .forEach(planElement -> {
                    if(planElement instanceof Activity){
                        Id<Link> linkId = ((Activity) planElement).getLinkId();
                        ((Activity) planElement).setCoord(network.getLinks().get(linkId).getCoord());
                    } else {
                        ((Leg) planElement).setRoute(null);
                    }
                });

        log.info("finished preparing scenario for freight...");
    }

    private static HashSet<String> getModesWithFreight(List<String> modes, Collection<String> allowedModes) {
        var newAllowedModes = new HashSet<String>();
        newAllowedModes.addAll(allowedModes);
        newAllowedModes.addAll(modes);
        return newAllowedModes;
    }

    public static List<String> getFreightModes(){
        return modes.stream().map(mode -> COMMERCIAL + "_" + mode).collect(Collectors.toList());
    }

    @Deprecated
    public static void adjustControlerForFreight(Controler controler, List<String> modes){
        for (String mode : modes) {
            controler.addOverridingModule( new AbstractModule() {
                @Override
                public void install() {
                    addTravelTimeBinding(mode).to(networkTravelTime());
                    addTravelDisutilityFactoryBinding(mode).to(carTravelDisutilityFactoryKey());
                }
            });
        }
    }
}
