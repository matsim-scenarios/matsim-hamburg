/**
 * 
 */
package org.matsim.analysis.pt;

import java.io.File;
import java.io.IOException;
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

//	public static void main(String[] args) {
//		String realWordCountsFile = "C:/Users/Aravind/work/Calibration/HVV-counts/HVV Fahrgastzahlen 2014-2020";
//		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> realWordCountsData = read(realWordCountsFile);
//	}
	static public LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> read(String directoryToScanForRuns) {

		LinkedHashMap<String, LinkedHashMap<String, PersonCounts>> ptCounts = new LinkedHashMap<String, LinkedHashMap<String, PersonCounts>>();
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
					// new row
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
							if (stop != null) {
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

		return ptCounts;
	}

	public static LinkedHashMap<String, String> mapLineNoYear(String directoryToScanForRuns) {
			LinkedHashMap<String, String> lineYear = new LinkedHashMap<String, String>();
			File[] files = new File(directoryToScanForRuns).listFiles(File::isFile);
			for (int k = 0; k < files.length; k++) {
				File file = new File(files[k].toString());
				String[] fileNameSplit = file.getName().split("_");
				String[] year = fileNameSplit[3].split("[.]");
				lineYear.put(fileNameSplit[2], year[0]);
			}
			return lineYear;
	}

}
