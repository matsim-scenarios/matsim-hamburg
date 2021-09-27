package org.matsim.analysis.accidents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.run.RunBaseCaseHamburgScenario;

import java.io.IOException;
import java.util.Set;

/**
 * @author ikaddoura, mmayobre, grybczak
 */

public class RunAccidentsHamburg {
    private static final Logger log = Logger.getLogger(RunAccidentsHamburg.class);

    public static void main(String[] args) throws IOException {
        if (args.length==0) {
            args = new String[] {"scenarios/input/hamburg-v1.1-1pct.config.xml"};
        }

        RunAccidentsHamburg main = new RunAccidentsHamburg();
        main.run(args);
    }

    private void run(String[] args) throws IOException {

        log.info("Loading scenario...");
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args);
        config.controler().setLastIteration(0);
        AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
        accidentsSettings.setEnableAccidentsModule(true);

        final Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        // Preprocess network
        Set<Id<Link>> tunnelLinks = HamburgAccidentsNetworkModification.readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_tunnel_2021.csv");
        Set<Id<Link>> planfreeLinks = HamburgAccidentsNetworkModification.readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_planfree_2021.csv");
        HamburgAccidentsNetworkModification.setLinkAttributesBasedOnInTownShapeFile(accidentsSettings, scenario.getNetwork(), "", tunnelLinks, planfreeLinks);

        //networkModification.setLinkAttributsBasedOnOSMFile("osmlandUseFile", "EPSG:31468" , tunnelLinks, planfreeLinks );
//        hamburgAccidentsNetworkModification.setLinkAttributesBasedOnOSMFile(null, null, tunnelLinks, planfreeLinks);

        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);
        controler.addOverridingModule(new AccidentsModule());
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();
    }



}