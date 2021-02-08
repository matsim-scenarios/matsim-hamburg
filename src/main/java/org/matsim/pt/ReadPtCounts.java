/**
 * 
 */
package org.matsim.pt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Aravind
 *
 */
public class ReadPtCounts {

	public static void main(String[] args) {
		String realWordCountsFile = "C:/Users/Aravind/work/Calibration/HVV-counts/HVV Fahrgastzahlen 2014-2020";
		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCountsData = read(realWordCountsFile);
	}
	static public LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> read(String directoryToScanForRuns) {

		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> ptCounts = new LinkedHashMap<String, LinkedHashMap<String, PersonCounts>>();
//		String directoryToScanForRuns = "C:/Users/Aravind/work/Calibration/HVV-counts/HVV Fahrgastzahlen 2014-2020";
		File[] files = new File(directoryToScanForRuns).listFiles(File::isFile);
		for (int k = 0; k < files.length; k++) {

			File file = new File(files[k].toString());
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder;
			LinkedHashMap<String, PersonCounts> trip = new LinkedHashMap<String, PersonCounts>();
			String lineNo = null;
			try {
				documentBuilder = documentBuilderFactory.newDocumentBuilder();
				Document document = documentBuilder.parse(file);
				Node table = document.getElementsByTagName("Table").item(0);
				NodeList tableChildren = table.getChildNodes();
				int count = 0;
				for (int i = 0; i < tableChildren.getLength(); i++) {
					//new row
					PersonCounts personCounts = new PersonCounts();
					Node tableItems = tableChildren.item(i);
					if (tableItems.getNodeName() == "Row") {
						NodeList children = tableItems.getChildNodes();
						if (count == 0) {
							lineNo = children.item(1).getTextContent();
							String[] lineNoSplit = lineNo.split(":");
							lineNo = lineNoSplit[1].trim();
						} else if (count > 5) {
							String stop = null;
							for (int j = 1; j < children.getLength(); j = j + 2) {
								Node childItem = children.item(j);
								if (!childItem.getTextContent().isEmpty()) {
									String value = childItem.getTextContent();
									if (j == 1) {
										stop = value;
									} else if (j == 3) {
										personCounts.setEinsteigerOutbound(Integer.parseInt(value));
									} else if (j == 5) {
										personCounts.setAussteigerOutbound(Integer.parseInt(value));
									} else if (j == 7) {
										personCounts.setEinsteigerInbound(Integer.parseInt(value));
									} else if (j == 9) {
										personCounts.setAussteigerInbound(Integer.parseInt(value));
									}
								}
							}
							if(stop != null) {
								trip.put(stop, personCounts);
							}
						}
						count++;
					}

				}
				ptCounts.put(lineNo, trip);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("Done, reading real world data");
		
		

		FileWriter fwriter;
		try {
			fwriter = new FileWriter(new File("C:/Users/Aravind/work/Calibration" + "/realworldLines.txt"), false);
			BufferedWriter bw = new BufferedWriter(fwriter);
			PrintWriter writer = new PrintWriter(bw);
			
			PrintWriter writer1 = null;
			for(String line : ptCounts.keySet()) {
				FileWriter fwriter1 = new FileWriter(new File("C:/Users/Aravind/work/Calibration/testreadptcounts/" +line+".csv"), false);
				BufferedWriter bw1 = new BufferedWriter(fwriter1);
				writer1 = new PrintWriter(bw1);
				
				LinkedHashMap<String, PersonCounts> trip = ptCounts.get(line);
				int i = 0;
				for(String station : trip.keySet()) {
					PersonCounts personCounts = trip.get(station);
					if(i == 0) {
						writer1.print("Station");
						writer1.print(",");
						writer1.print("Einsteiger Outbound");
						writer1.print(",");
						writer1.print("Aussteiger Outbound");
						writer1.print(",");
						writer1.print("Einsteiger Inbound");
						writer1.print(",");
						writer1.print("Aussteiger Inbound");
						writer1.print(",");
					}
					
					writer1.println();
					writer1.print(station);
					writer1.print(",");
					writer1.print(personCounts.getEinsteigerOutbound());
					writer1.print(",");
					writer1.print(personCounts.getAussteigerOutbound());
					writer1.print(",");
					writer1.print(personCounts.getEinsteigerInbound());
					writer1.print(",");
					writer1.print(personCounts.getAussteigerInbound());
					writer1.print(",");
					
					i++;
				}
				
				writer1.println();
				writer1.println();
				
				writer1.flush();
				writer1.close();
			}
			
			
			
			for(String key : ptCounts.keySet()) {
				writer.println(key);
			}
			
			writer.flush();
			writer.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Done writing");
		return ptCounts;
	}
}
