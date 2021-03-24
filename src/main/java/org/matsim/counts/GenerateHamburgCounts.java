/**
 * 
 */
package org.matsim.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author Aravind
 *
 */
public class GenerateHamburgCounts {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Counts<Link> countspkw = new Counts<>();
		Counts<Link> countslkw = new Counts<>();
		Counts<Link> countssum = new Counts<>();
		String stationName = "test";
		countspkw.setYear(2019);
		countspkw.setName("2019");
		countslkw.setYear(2019);
		countslkw.setName("2019");
		countssum.setYear(2019);
		countssum.setName("2019");

		String outputpkw = "D:/Work/Count mapping for Hamburg/Generate counts file/counts-pkw.xml";
		String outputlkw = "D:/Work/Count mapping for Hamburg/Generate counts file/counts-lkw.xml";
		String outputsum = "D:/Work/Count mapping for Hamburg/Generate counts file/counts-sum.xml";

		String fileName = "D:/Work/Count mapping for Hamburg/Generate counts file/DZS-FHH-2019_Einzelne-Richtungen1.csv";
		HashMap<String, String> mapMatch = readMapMatchFile();
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
			String line = null;
			String nextLine = null;
			String[] direction = null;
			Count<Link> countpkw;
			Count<Link> countlkw;
			Count<Link> countsum;
			while ((line = bufferedReader.readLine()) != null) {
				String[] splitValue = line.split(",");
				if (splitValue.length > 0 && splitValue[0].contains("Zählstelle")) {
					String[] split = splitValue[0].split("\\s+");
					String key = split[1];
					nextLine = bufferedReader.readLine();
					direction = nextLine.split("\\s+");
					key = key + direction[2] + direction[4];
					String newKey = key.replace(",", "");
					final Id<Link> link = Id.create(mapMatch.get(newKey), Link.class);
					countpkw = countspkw.createAndAddCount(link, stationName);
					countlkw = countslkw.createAndAddCount(link, stationName);
					countsum = countssum.createAndAddCount(link, stationName);

					while ((line = bufferedReader.readLine()) != null) {
						nextLine = line;
						splitValue = nextLine.split(",");
						if (splitValue.length > 0 && splitValue[0].contains("Zeit")) {
							nextLine = bufferedReader.readLine();
							int i = 0;
							do {
								line = bufferedReader.readLine();
								splitValue = line.split(",");
								countpkw.createVolume(10, 123);
								countpkw.createVolume(Integer.parseInt(splitValue[0].split(":")[0]),
										Double.valueOf(splitValue[1]));
								countlkw.createVolume(Integer.parseInt(splitValue[0].split(":")[0]),
										Double.valueOf(splitValue[2]));
								countsum.createVolume(Integer.parseInt(splitValue[0].split(":")[0]),
										Double.valueOf(splitValue[1]) + Double.valueOf(splitValue[2]));
								if (splitValue[0].contains("24:00:00"))
									i = 1;
							} while (i == 0);
							break;
						}
					}
				}
			}
			new CountsWriter(countspkw).write(outputpkw);
			new CountsWriter(countslkw).write(outputlkw);
			new CountsWriter(countssum).write(outputsum);
			bufferedReader.close();
			System.out.println("Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static HashMap<String, String> readMapMatchFile() {

		String fileName = "D:/Work/Count mapping for Hamburg/Generate counts file/mapmatch BVM traffic counts1.csv";
		HashMap<String, String> mapMatch = new HashMap<String, String>();
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				String[] splitValue = line.split(",");
				if (splitValue.length > 0 && splitValue[0].contains("Zählstelle")) {
					String[] direction = splitValue[3].split("\\s+");
					String key = splitValue[0].split("\\s+")[1] + direction[2] + direction[4];
					String value = splitValue[4];
					mapMatch.put(key, value);
				}
			}
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mapMatch;
	}

}
