package org.matsim.prepare;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.sharing.io.ImmutableSharingStationSpecification;
import org.matsim.contrib.sharing.io.ImmutableSharingVehicleSpecification;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.io.SharingServiceWriter;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class CreateSharingService {

	public static void main(String[] args) {
		double lat1 = 53.553098;
		double lon1 = 10.02397;
		double lat2 = 53.571746;
		double lon2 = 10.068886;
		double lat3 = 53.588644;
		double lon3 = 10.033012;
		Coord coord1 = new Coord(lon1, lat1);
		Coord coord2 = new Coord(lon2, lat2);
		Coord coord3 = new Coord(lon3, lat3);
		Map<String, Coord> location = new HashMap<>();
		location.put("loc1", coord1);
		location.put("loc2", coord2);
		location.put("loc3", coord3);
		
		String networkPath = "C:/Users/Aravind/work/Calibration/25percent/hamburg-v1.1-25pct.output_network.xml.gz";
		String outputPath = "C:/Users/Aravind/work/Calibration/Sharing/test.xml";
		
//		String networkPath = args[0];
//		String outputPath = args[1];
//		String capacity = args[2]; equals no: of vehicles
		Network fullNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(fullNetwork).readFile(networkPath);
		SharingServiceSpecification service = new DefaultSharingServiceSpecification();
		int stationId = 1;
		int vehicleId = 1;
		for(Entry<String, Coord> e : location.entrySet()) {
			Link link = NetworkUtils.getNearestLink(fullNetwork, e.getValue());
			
			service.addStation(ImmutableSharingStationSpecification.newBuilder() //
					.id(Id.create(stationId, SharingStation.class)) //
					.capacity(20) //
					.linkId(link.getId()) //
					.build());
			//loop
			service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder() //
					.id(Id.create(vehicleId, SharingVehicle.class)) //
					.startStationId(Id.create(1, SharingStation.class)) //
					.startLinkId(link.getId()) //
					.build());
			stationId++;
			vehicleId++;
		}
		
		new SharingServiceWriter(service).write(outputPath);
		System.out.println("done!!");

	}

}
