package org.matsim.parking;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public void addLinkParkPressureAsAttribute(Double[] parkPressureBasedParkTime) throws IOException {
        this.readLink2ParkPressure();
        for (Link link :
                this.network.getLinks().values()) {
            String attribute = "ParkPressure";
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

        File file = new File(this.linkId2ParkPressureCSVFile);
        log.info("read linkId2ParkPressure from" + file.getAbsolutePath());

        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String firstLine = csvReader.readLine();
        while ((firstLine = csvReader.readLine()) != null) {
            String[] income = firstLine.split(",");
           this.link2ParkPressure.put(income[0],Double.valueOf(income[1]));
        }
        csvReader.close();
    }

}
