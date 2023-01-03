package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.commons.nullanalysis.NotNull;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.misc.Counter;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.prepare.drt.HamburgShpUtils;
import org.matsim.prepare.pt.ClassifyStationType;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tschlenther
 */
public class RunDRTHamburgScenario {

    private static final Logger log = LogManager.getLogger(RunDRTHamburgScenario.class);

    private static final String DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE = "intermodalPtDrtStop";
    private static final String DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE = "railway-subway";

    public static void main(String[] args) throws ParseException, IOException {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
//            args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v3/v3.0/input/reallab2030/hamburg-v3.0-10pct.config.reallabHH2030.xml"};
            args = new String[] {"scenarios/input/hamburg-v4.0-1pct.config.drt.xml"};
        }

        new RunDRTHamburgScenario().run(args);
    }

    private void run(String[] args) throws IOException {

        Config config = prepareConfig(args);

        Scenario scenario = prepareScenario(config);

        Controler controler = prepareControler(scenario);

        controler.run();
        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        Controler controler =  RunBaseCaseHamburgScenario.prepareControler(scenario);
        prepareControler(controler);
        return controler;
    }

    public static void prepareControler(Controler controler) {
        //drt + dvrp module
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());

        //configureQSim: activate all dvrp modes
        MultiModeDrtConfigGroup mmCfg = MultiModeDrtConfigGroup.get(controler.getConfig());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(mmCfg));

        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                //need to bind this in another overriding module than in the module where we install the SwissRailRaptorModule
                bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
            }
        });

        controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
        controler.addOverridingModule(new PtIntermodalRoutingModesModule());
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {
        ConfigGroup[] customModulesToAdd = new ConfigGroup[] { new DvrpConfigGroup(), new MultiModeDrtConfigGroup(),
                new SwissRailRaptorConfigGroup(), new IntermodalTripFareCompensatorsConfigGroup(),
                new PtIntermodalRoutingModesConfigGroup()};
        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args, customModulesAll);

        //when simulating dvrp, we need/should simulate from the start to the end
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(), config.plansCalcRoute());

        return config;
    }

    /**
     * loads the scenario based on the {@code config}. Then adds the drt modes to all links inside the service area, if defined and necessary.
     * All rail and subway transit stops inside the corresponding service area are marked as intermodal access/egress stops.
     * @param config
     * @return
     * @throws IOException
     */
    public static Scenario prepareScenario(Config config) throws IOException {
        Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);
        return prepareNetwork(scenario);
    }

    public static Scenario prepareNetwork(Scenario scenario) {
        HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), HamburgExperimentalConfigGroup.class);
        DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), DvrpConfigGroup.class);

        for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(scenario.getConfig()).getModalElements()) {
            String drtServiceAreaShapeFile = drtCfg.drtServiceAreaShapeFile;
            if(dvrpConfig.networkModes.contains(drtCfg.getMode()) || drtCfg.useModeFilteredSubnetwork){
                if (drtServiceAreaShapeFile != null && !drtServiceAreaShapeFile.equals("") && !drtServiceAreaShapeFile.equals("null")) {
                    addDRTmode(scenario, drtCfg.getMode(), drtServiceAreaShapeFile, 1000.);
                }
            }
            //tag pt stops that are to be used for intermodal access and egress
            tagTransitStopsInServiceArea(scenario.getTransitSchedule(),
                    DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_ATTRIBUTE, DRT_ACCESS_EGRESS_TO_PT_STOP_FILTER_VALUE,
                    drtCfg.drtServiceAreaShapeFile,
                    // This does not mean that a drt vehicle can pick the passenger up outside the service area,
                    // rather the passenger has to walk the last few meters from the drt drop off to the station.
                    250.); //
        }
        return scenario;
    }

    public static void addDRTmode(Scenario scenario, @NotNull String drtNetworkMode, @NotNull String drtServiceAreaShapeFile, double buffer) {
        Preconditions.checkNotNull(drtServiceAreaShapeFile,"you have to provide a shape file that defines where the mode " +
                drtNetworkMode + " is supposed to get allowed on the network.");

        log.info("Adjusting network...");

        HamburgShpUtils shpUtils = new HamburgShpUtils( drtServiceAreaShapeFile );

        int counterInside = 0;
        int counterOutside = 0;

        Counter counter = new Counter("adjusting link #");
        for (Link link : scenario.getNetwork().getLinks().values()) {
            counter.incCounter();
            if (link.getAllowedModes().contains(TransportMode.car)) {
                if (shpUtils.isCoordInDrtServiceAreaWithBuffer(link.getFromNode().getCoord(), buffer)
                        || shpUtils.isCoordInDrtServiceAreaWithBuffer(link.getToNode().getCoord(), buffer)) {
                    Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

                    allowedModes.add(drtNetworkMode);

                    link.setAllowedModes(allowedModes);
                    counterInside++;
                } else {
                    counterOutside++;
                }

            } else if (link.getAllowedModes().contains(TransportMode.pt)) {
                // skip pt links
            } else {
                throw new RuntimeException("Aborting...");
            }
        }

        log.info("Total links: " + counter.getCounter());
        log.info("Total links inside service area: " + counterInside);
        log.info("Total links outside service area: " + counterOutside);

        Set<String> modes = new HashSet<>();
        modes.add(drtNetworkMode);
        new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
    }

    private static void tagTransitStopsInServiceArea(TransitSchedule transitSchedule,
                                                     String newAttributeName, String newAttributeValue,
                                                     String drtServiceAreaShapeFile,
                                                     /*String oldFilterAttribute, String oldFilterValue,*/
                                                     double bufferAroundServiceArea) {
        log.info("Tagging pt stops marked for intermodal access/egress in the service area.");
        HamburgShpUtils shpUtils = new HamburgShpUtils( drtServiceAreaShapeFile );

        //TODO this rather ugly classification implementation should be replaced by GL's version which directly puts attributes into the transitSchedule
        ClassifyStationType.getStop2TypesList(transitSchedule).stream()
                .filter(stop2type -> stop2type.getType() != null && (stop2type.getType().contains("s") || stop2type.getType().contains("r")) ) //filter rail and subway stations
                .filter(stop2Type -> shpUtils.isCoordInDrtServiceAreaWithBuffer(stop2Type.getStop().getCoord(), bufferAroundServiceArea)) //filter spatially
                .forEach(stop2type -> stop2type.getStop().getAttributes().putAttribute(newAttributeName, newAttributeValue));

    }
}
