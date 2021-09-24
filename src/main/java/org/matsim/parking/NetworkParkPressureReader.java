package org.matsim.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.HamburgExperimentalConfigGroup;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.parking.UtilityBasedParkingPressureEventHandler.parkPressureAttributeName;

/**
 * @author zmeng
 */
public class NetworkParkPressureReader {
    private static final Logger log = Logger.getLogger(NetworkParkPressureReader.class);

    private final Network network;
    private final HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup;

    public NetworkParkPressureReader(Network network, HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup) {
        this.network = network;
        this.hamburgExperimentalConfigGroup = hamburgExperimentalConfigGroup;
    }

    private final Map<String, Double> link2ParkPressure = new HashMap<>();

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        HamburgExperimentalConfigGroup cfg = new HamburgExperimentalConfigGroup();
        cfg.setParkPressureLinkAttributeFile("D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt.link2ParkPressure.csv");
        NetworkParkPressureReader networkParkPressureReader = new NetworkParkPressureReader(network,cfg);
        networkParkPressureReader.addLinkParkTimeAsAttribute();

//        for (Link link :
//                network.getLinks().values()) {
//            if (!link.getAttributes().getAsMap().containsKey("parkTime"))
//                System.out.println(link.getId());
//        }
        network.getAttributes().putAttribute("coordinateReferenceSystem", "EPSG:25832");
        NetworkUtils.writeNetwork(network,"D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt-and-parkingPressure.xml.gz");
        System.out.println("done");
    }

    public void addLinkParkTimeAsAttribute() throws IOException {
        String[] parkPressureScoreParams = this.hamburgExperimentalConfigGroup.getParkPressureScoreParams().split(",");
        double parkPressureScoreConstant = this.hamburgExperimentalConfigGroup.getParkPressureScoreConstant();
        this.readLink2ParkPressure();
        for (Link link :
                this.network.getLinks().values()) {
            String attribute = parkPressureAttributeName;
            if (!this.link2ParkPressure.containsKey(link.getId().toString())) {
                link.getAttributes().putAttribute(attribute, Double.parseDouble(parkPressureScoreParams[2]) * parkPressureScoreConstant);
            } else {
                Double parkPressure = link2ParkPressure.get(link.getId().toString());
                if (parkPressure == 0.7) {
                    link.getAttributes().putAttribute(attribute, Double.parseDouble(parkPressureScoreParams[0]) * parkPressureScoreConstant);
                } else if (parkPressure == 0.85) {
                    link.getAttributes().putAttribute(attribute, Double.parseDouble(parkPressureScoreParams[1]) * parkPressureScoreConstant);
                }
            }
        }
    }

    private void readLink2ParkPressure() throws IOException {
        BufferedReader csvReader;
        String linkId2ParkPressureCSVFile = hamburgExperimentalConfigGroup.getParkPressureLinkAttributeFile();

            log.info("Adding missing park pressure link attributes based on provided files...");
            log.info("read linkId2ParkPressure from " + linkId2ParkPressureCSVFile);
            if(linkId2ParkPressureCSVFile.contains("https://")){
                URL url = new URL(linkId2ParkPressureCSVFile);
                URLConnection yc = url.openConnection();
                csvReader = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            } else {
                File file = new File(linkId2ParkPressureCSVFile);
                csvReader = new BufferedReader(new FileReader(file));
            }

            String firstLine = csvReader.readLine();
            while ((firstLine = csvReader.readLine()) != null) {
                String[] parkPressure = firstLine.split(",");
                this.link2ParkPressure.put(parkPressure[0],Double.valueOf(parkPressure[1]));
            }
            csvReader.close();
            log.info("Adding missing park pressure link attributes based on provided files... Done.");

    }

}
