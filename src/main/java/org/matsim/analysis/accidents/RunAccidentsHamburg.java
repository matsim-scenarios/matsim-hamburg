package org.matsim.analysis.accidents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.matsim.run.reallabHHPolicyScenarios.RunReallabHH2030Scenario;

import java.io.IOException;
import java.util.Set;

/**
 * @author ikaddoura, mmayobre, grybczak, tschlenther
 *
 * taken from accidents contrib and adjusted
 */

public class RunAccidentsHamburg {
    private static final Logger log = Logger.getLogger(RunAccidentsHamburg.class);

    private static final boolean PREPROCESS_NETWORK_DEFAULT = true;
    private static final boolean BASE_CASE_DEFAULT = true;
    private static final String CONFIG = "provide config";
    private static final double SCALE_FACTOR = 10.0; //set according to sample size

    public static void main(String[] args) throws IOException {
        boolean preProcessNetwork;
        boolean baseCase;
        String[] configArgs;
        if (args.length==0) {
            baseCase = BASE_CASE_DEFAULT;
            preProcessNetwork = PREPROCESS_NETWORK_DEFAULT;
            configArgs = new String[] {CONFIG};
        } else {
            baseCase = Boolean.parseBoolean(args[0]);
            preProcessNetwork = Boolean.parseBoolean(args[1]);
            configArgs = new String[args.length -2];
            for (int i = 2; i < args.length; i++) {
                configArgs[i-2] = args[i];
            }
        }
        Config config;
        if(baseCase){
            config = RunBaseCaseHamburgScenario.prepareConfig(configArgs);
        } else {
            config = RunReallabHH2030Scenario.prepareConfig(configArgs);
        }
        config.plans().setInputFile(config.controler().getRunId() + ".output_plans.xml.gz");
        String outputDir = CONFIG.substring(0, CONFIG.lastIndexOf( '/') + 1) + "accidentsAnalysis/";
        config.controler().setOutputDirectory(outputDir);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setLastIteration(0);
        config.strategy().setFractionOfIterationsToDisableInnovation(0);
        config.travelTimeCalculator().setTraveltimeBinSize(2*3600);
        AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
        accidentsSettings.setScaleFactor(SCALE_FACTOR);
        accidentsSettings.setEnableAccidentsModule(true);

        log.info("Loading scenario...");
        final Scenario scenario;
        // Preprocess network
        if(preProcessNetwork){
            if(baseCase){
                scenario =  RunBaseCaseHamburgScenario.prepareScenario(config);
            } else {
                scenario =  RunReallabHH2030Scenario.prepareScenario(config);
            }
            Network networkWithRealisticNumberOfLanes = NetworkUtils.readTimeInvariantNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.0/hamburg-v1.0-network-with-pt.xml.gz");
            Set<Id<Link>> tunnelLinks;
            if(baseCase){
                //2021
                tunnelLinks = HamburgAccidentsNetworkModification.readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_tunnel_2021.csv");
            } else {
                //2030
                tunnelLinks = HamburgAccidentsNetworkModification.readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_tunnel_2030.csv");
            }
            Set<Id<Link>> planfreeLinks = HamburgAccidentsNetworkModification.readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_planfree_2021.csv");
            HamburgAccidentsNetworkModification.setLinkAttributesBasedOnInTownShapeFile(accidentsSettings,
                    scenario.getNetwork(),
                    networkWithRealisticNumberOfLanes,
                    "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/shp/innerorts-ausserorts/hamburg_hvv_innerorts_inkl_HH_city_reduced.shp",
                    tunnelLinks,
                    planfreeLinks);
        } else {
            //plan free and tunnel links as of 2021
            config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-with-pt-with-accidentAttributes-2021.xml.gz");
            if(baseCase){
                scenario =  RunBaseCaseHamburgScenario.prepareScenario(config);
            } else {
                scenario =  RunReallabHH2030Scenario.prepareScenario(config);
            }
        }

        Controler controler;
        if(baseCase){
            controler =  RunBaseCaseHamburgScenario.prepareControler(scenario);
        } else {
            controler =  RunReallabHH2030Scenario.prepareControler(scenario);
        }
        controler.addOverridingModule(new AccidentsModule());
        controler.run();
    }




}