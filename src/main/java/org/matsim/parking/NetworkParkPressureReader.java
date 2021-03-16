package org.matsim.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zmeng
 */
public class NetworkParkPressureReader {
    private static final Logger log = Logger.getLogger(NetworkParkPressureReader.class);

    private final Network network;
    private final String linkId2ParkPressureCSVFile;
    private final Map<String, Double> link2ParkPressure = new HashMap<>();

    public NetworkParkPressureReader(Network network, String linkId2ParkPressureCSVFile) {
        this.network = network;
        this.linkId2ParkPressureCSVFile = linkId2ParkPressureCSVFile;
    }

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("/Users/meng/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1.0-1pct/input/hamburg-v1.0-network-with-pt.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        NetworkParkPressureReader networkParkPressureReader = new NetworkParkPressureReader(network,"/Users/meng/shared-svn/projects/matsim-hamburg/hamburg-v1.0/network_specific_info/link2parkpressure.csv");
        networkParkPressureReader.addLinkParkTimeAsAttribute(new Double[]{1200.,720.,0.});

        for (Link link :
                network.getLinks().values()) {
            if (!link.getAttributes().getAsMap().containsKey("parkTime"))
                System.out.println(link.getId());
        }
        System.out.println("done");
        //NetworkUtils.writeNetwork(network,"/Users/meng/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1.0-1pct/input/hamburg-v1.0-network-with-pt-park.xml.gz");


    }

    public void addLinkParkTimeAsAttribute(Double[] parkPressureBasedParkTime) throws IOException {
        this.readLink2ParkPressure();
        for (Link link :
                this.network.getLinks().values()) {
            String attribute = "parkTime";
            if (!this.link2ParkPressure.containsKey(link.getId().toString())) {
                link.getAttributes().putAttribute(attribute, parkPressureBasedParkTime[2]);
            } else {
                Double parkPressure = link2ParkPressure.get(link.getId().toString());
                if (parkPressure == 0.7) {
                    link.getAttributes().putAttribute(attribute, parkPressureBasedParkTime[0]);
                } else if (parkPressure == 0.85) {
                    link.getAttributes().putAttribute(attribute, parkPressureBasedParkTime[1]);
                }
            }
        }
    }

    private void readLink2ParkPressure() throws IOException {
        BufferedReader csvReader;

        log.info("read linkId2ParkPressure from " + this.linkId2ParkPressureCSVFile);
        if(this.linkId2ParkPressureCSVFile.contains("https://")){
            URL url = new URL(this.linkId2ParkPressureCSVFile);
            URLConnection yc = url.openConnection();
            csvReader = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        } else {
            File file = new File(this.linkId2ParkPressureCSVFile);
            csvReader = new BufferedReader(new FileReader(file));
        }

        String firstLine = csvReader.readLine();
        while ((firstLine = csvReader.readLine()) != null) {
            String[] income = firstLine.split(",");
           this.link2ParkPressure.put(income[0],Double.valueOf(income[1]));
        }
        csvReader.close();
    }

}
