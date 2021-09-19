package org.matsim.analysis.accidents;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.contrib.accidents.AccidentsContext;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunBaseCaseHamburgScenario;

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

    private void run(String[] args) throws MalformedURLException, IOException {

        log.info("Loading scenario...");
        Config config = RunBaseCaseHamburgScenario.prepareConfig(args);
        config.controler().setLastIteration(0);
        AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
        accidentsSettings.setEnableAccidentsModule(true);

        final Scenario scenario = RunBaseCaseHamburgScenario.prepareScenario(config);

        // Preprocess network
        HamburgAccidentsNetworkModification hamburgAccidentsNetworkModification = new HamburgAccidentsNetworkModification(scenario);

        //String[] tunnelLinks = readCSVFile("tunnels.csv");
        //String[] planfreeLinks = readCSVFile("planfreeLinks.csv");


        //networkModification.setLinkAttributsBasedOnOSMFile("osmlandUseFile", "EPSG:31468" , tunnelLinks, planfreeLinks );
        hamburgAccidentsNetworkModification.setLinkAttributsBasedOnOSMFile(null, null, null, null);
        Controler controler = RunBaseCaseHamburgScenario.prepareControler(scenario);
        controler.addOverridingModule(new AccidentsModule());
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();
    }

    private String[] readCSVFile(String csvFile) {
        ArrayList<Id<Link>> links = new ArrayList<>();

        BufferedReader br = IOUtils.getBufferedReader(csvFile);

        String line = null;
        try {
            line = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            int countWarning = 0;
            while ((line = br.readLine()) != null) {

                String[] columns = line.split(";");
                Id<Link> linkId = null;
                for (int column = 0; column < columns.length; column++) {
                    if (column == 0) {
                        linkId = Id.createLinkId(columns[column]);
                    } else {
                        if (countWarning < 1) {
                            log.warn("Expecting the link Id to be in the first column. Ignoring further columns...");
                        } else if (countWarning == 1) {
                            log.warn("This message is only given once.");
                        }
                        countWarning++;
                    }
                }
                log.info("Adding link ID " + linkId);
                links.add(linkId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] linkIDsArray = (String[]) links.toArray();
        return linkIDsArray ;
    }

}