package org.matsim.parking;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

    private Network network;
    private HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup;

    public NetworkParkPressureReader(Network network, HamburgExperimentalConfigGroup hamburgExperimentalConfigGroup) {
        this.network = network;
        this.hamburgExperimentalConfigGroup = hamburgExperimentalConfigGroup;
    }

    private final Map<String, Double> link2ParkPressure = new HashMap<>();

    public static void main(String[] args) throws IOException {
//        Config config = ConfigUtils.createConfig();
//        config.network().setInputFile("/Users/meng/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1.0-1pct/input/hamburg-v1.0-network-with-pt.xml.gz");
//        Scenario scenario = ScenarioUtils.loadScenario(config);
//        Network network = scenario.getNetwork();
//        NetworkParkPressureReader networkParkPressureReader = new NetworkParkPressureReader(network,"/Users/meng/shared-svn/projects/matsim-hamburg/hamburg-v1.0/network_specific_info/link2parkpressure.csv");
//        networkParkPressureReader.addLinkParkTimeAsAttribute(new Double[]{1200.,720.,0.});
////        networkParkPressureReader.addLinkParkTimeAsAttribute(new Double[]{1.,0.5,0.});
//
//        for (Link link :
//                network.getLinks().values()) {
//            if (!link.getAttributes().getAsMap().containsKey("parkTime"))
//                System.out.println(link.getId());
//        }
//        System.out.println("done");
//        //NetworkUtils.writeNetwork(network,"/Users/meng/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1.0-1pct/input/hamburg-v1.0-network-with-pt-park.xml.gz");
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
