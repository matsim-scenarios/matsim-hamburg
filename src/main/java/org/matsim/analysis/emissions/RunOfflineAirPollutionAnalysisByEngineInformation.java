/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.emissions;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.HamburgExperimentalConfigGroup;
import org.matsim.vehicles.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.contrib.emissions.Pollutant.*;

/**
 *
 * Computes the emissions using the 'allVehicles' output file
 *
 * @author tschlenther
 */

public class RunOfflineAirPollutionAnalysisByEngineInformation {
	private static final Logger log = Logger.getLogger(RunOfflineAirPollutionAnalysisByEngineInformation.class);

	private final String runDirectory;
	private final String runId;
	private final String hbefaWarmFile;
	private final String hbefaColdFile;
	private final String analysisOutputDirectory;
	private final EnumeratedDistribution heavyGoodsVehicleTypeDistribution;
	private final EnumeratedDistribution lightCommercialVehicleTypeDistribution;
	private final EnumeratedDistribution passengerVehicleTypeDistribution;

	static List<Pollutant> pollutants2Output = Arrays.asList(CO2_TOTAL, NOx, PM, PM_non_exhaust);

	//TODO maybe use a builder
	RunOfflineAirPollutionAnalysisByEngineInformation(String runDirectory, String runId, String hbefaFileWarm, String hbefaFileCold, String analysisOutputDirectory, List<Pair<String, Double>> passengerCarEmissionConceptShares, List<Pair<String, Double>> heavyGoodsVehiclesEmissionConceptShares, List<Pair<String, Double>> lightCommercialVehiclesEmissionConceptShares) {
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		this.runDirectory = runDirectory;

		this.runId = runId;
		this.hbefaWarmFile = hbefaFileWarm;
		this.hbefaColdFile = hbefaFileCold;

		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;

		this.heavyGoodsVehicleTypeDistribution = new EnumeratedDistribution(heavyGoodsVehiclesEmissionConceptShares);
		this.lightCommercialVehicleTypeDistribution = new EnumeratedDistribution(lightCommercialVehiclesEmissionConceptShares);
		this.passengerVehicleTypeDistribution = new EnumeratedDistribution(passengerCarEmissionConceptShares);
	}

	public static void main(String[] args) throws IOException {

		//TODO: Please set MATSIM_DECRYPTION_PASSWORD as environment variable to decrypt the files. ask VSP for access.

		final String hbefaPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/";
		//actually the hbefa files need to be set relative to the config or by absolute path...
//		final String hbefaFileCold = hbefaPath + "0e73947443d68f95202b71a156b337f7f71604ae/5a297db51545335b2f7899002a1ea6c45d4511a3.enc";
		final String hbefaFileCold = "../../svn/shared-svn/projects/matsim-germany/hbefa/hbefa-files/v4.1/EFA_ColdStart_Concept_2020_detailed_perTechAverage_withHGVetc.csv";
		final String hbefaFileWarm = hbefaPath + "0e73947443d68f95202b71a156b337f7f71604ae/944637571c833ddcf1d0dfcccb59838509f397e6.enc";

		final String hbefaFileCold_2030 = hbefaPath + "6d425121249f0be3f411175b88cf7551e24f7143/d1944abead553305d9f1c4131cadbd382655f592.enc";
		final String hbefaFileWarm_2030 = hbefaPath + "6d425121249f0be3f411175b88cf7551e24f7143/c154fc5d5ca7471c232f1b602575bdabbda26fab.enc";

		final String runId = "hamburg-v2.2-proKlima2030" ;
		String runDirectory = "provide run output directory";
		RunOfflineAirPollutionAnalysisByEngineInformation analysis = new RunOfflineAirPollutionAnalysisByEngineInformation(
				runDirectory,
				runId,
				hbefaFileWarm,
				hbefaFileCold,
				runDirectory + "emission-analysis-hbefa-v4.1-2020",
				HBEFAEmissionConceptShares.HBEFA_PSNGCAR_SHARE_2020,
				HBEFAEmissionConceptShares.HBEFA_HGV_SHARE_2020,
				HBEFAEmissionConceptShares.HBEFA_LCV_SHARE_2020);
		try {
			analysis.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void run() throws IOException {


		//lets load the actual output config instead of filling a dummy one. Hopefully this does not size up the scenario too much. This way, we can get access to
		//actually used values such as freeeSpeedFactor in HamburgExperimentalConfigGroup
		Config config = ConfigUtils.createConfig();
//		Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");

		config.vehicles().setVehiclesFile( runDirectory + runId + ".output_allVehicles.xml.gz");
		config.network().setInputFile( runDirectory +runId + ".output_network.xml.gz");
		config.transit().setTransitScheduleFile( runDirectory +runId + ".output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile( runDirectory + runId + ".output_transitVehicles.xml.gz");
		config.global().setCoordinateSystem("EPSG:25832");
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);

		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort);
		eConfig.setDetailedColdEmissionFactorsFile(hbefaColdFile);
		eConfig.setDetailedWarmEmissionFactorsFile(hbefaWarmFile);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);
		eConfig.setWritingEmissionsEvents(true); //in the end, the EventXMLWriter is currently out commented (see below)

		//TODO ?????
		eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none);

		HamburgExperimentalConfigGroup hamburgCfg = ConfigUtils.addOrGetModule(config, HamburgExperimentalConfigGroup.class);

		File folder = new File(analysisOutputDirectory);
		folder.mkdirs();

		final String eventsFile = runDirectory + runId + ".output_events.xml.gz";

		final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
		final String linkEmissionAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLink.csv";
		final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + runId + ".emissionsPerLinkPerM.csv";
		final String vehicleTypeFile = analysisOutputDirectory + runId + ".emissionVehicleInformation.csv";

		Scenario scenario = ScenarioUtils.loadScenario(config);
		prepareNetwork(hamburgCfg, scenario);

		//create vehicle types and add them to the scenario and store them in maps

		//passenger car vehicle types
		Map<String, VehicleType> passengerVehicleTypes = createAndAddVehicleTypesForAllEmissionConcepts(scenario, HbefaVehicleCategory.PASSENGER_CAR);
		//light commercial vehicle types
		Map<String, VehicleType> lightCommercialVehicleTypes = createAndAddVehicleTypesForAllEmissionConcepts(scenario, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE);
		//heavy goods vehicle types
		Map<String, VehicleType> heavyGoodsVehicleTypes = createAndAddHeavyGoodsVehicleTypesForAllEmissionConcepts(scenario);

		VehicleType defaultCarVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class));

		//transit vehicles
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			if (scenario.getTransitVehicles().getVehicleTypes().containsKey(type.getId())) {
				// consider transit vehicles as non-hbefa vehicles
				VehicleUtils.setHbefaVehicleCategory( type.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			}
		}

		//analyze current distribution
		scenario.getVehicles().getVehicles().values().stream()
				.map(vehicle -> { //map vehicle to category
					if(scenario.getTransitVehicles().getVehicles().get(vehicle.getId()) != null) return "transit";
					else if(vehicle.getId().toString().contains("freight") || vehicle.getId().toString().contains("commercial")) return "freight";
					else if(vehicle.getId().toString().contains("drt") || vehicle.getId().toString().contains("taxi")) return "drt";
					else if(vehicle.getType().getId().toString().equals(defaultCarVehicleType.getId().toString())) return "standard (private)";
					else return "unknown";
				})
				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
				.entrySet()
				.forEach(entry -> log.info("nr of " + entry.getKey() + " vehicles = " + entry.getValue()));

		// change some vehicle types, e.g. to investigate decarbonization scenarios, or to account for electric drt vehicles
		changeVehicleTypes(scenario, passengerVehicleTypes, heavyGoodsVehicleTypes, lightCommercialVehicleTypes);

		//analyze current distribution
		scenario.getVehicles().getVehicles().values().stream()
				.map(vehicle -> { //map vehicle to category
					if(scenario.getTransitVehicles().getVehicles().get(vehicle.getId()) != null) return "transit";
					else if(heavyGoodsVehicleTypes.containsValue(vehicle.getType())) return "heavy good";
					else if(lightCommercialVehicleTypes.containsValue(vehicle.getType())) return "light commercial";
					else if(vehicle.getId().toString().contains("drt") || vehicle.getId().toString().contains("taxi")) return "drt";
					else if(passengerVehicleTypes.containsValue(vehicle.getType())) return "passenger";
					else if(vehicle.getType().getId().toString().equals(defaultCarVehicleType.getId().toString())) return "still standard ???";
					else return "unknown // not assigned";
				})
				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
				.entrySet()
				.forEach(entry -> log.info("nr of " + entry.getKey() + " vehicles = " + entry.getValue()));

		//------------------------------------------------------------------------------

		// the following is copy paste from the example...
		EventsManager eventsManager = EventsUtils.createEventsManager();
		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);
		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

//        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
//        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		EmissionsOnLinkHandler emissionsEventHandler = new EmissionsOnLinkHandler();
		eventsManager.addHandler(emissionsEventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		log.info("Done reading the events file.");
		log.info("Finish processing...");
		eventsManager.finishProcessing();

//        log.info("Closing events file...");
//        emissionEventWriter.closeFile();

		writeOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, vehicleTypeFile, scenario, emissionsEventHandler);

		int totalVehicles = scenario.getVehicles().getVehicles().size();
		log.info("Total number of vehicles: " + totalVehicles);


		scenario.getVehicles().getVehicles().values().stream()
				.map(vehicle -> vehicle.getType())
				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
				.entrySet()
				.forEach(entry -> log.info("nr of " + VehicleUtils.getHbefaVehicleCategory(entry.getKey().getEngineInformation()) + " vehicles running on " + VehicleUtils.getHbefaEmissionsConcept(entry.getKey().getEngineInformation())
						+" = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "% overall)"));

//		scenario.getVehicles().getVehicles().values().stream()
//				.map(vehicle -> {
//					return VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) == null ? "NONE" : VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation());
//				})
//				.collect(Collectors.groupingBy(category -> category, Collectors.counting()))
//				.entrySet()
//				.forEach(entry -> log.info("nr of " + entry.getKey() + " vehicles = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "%)"));
	}

	private Map<String, VehicleType> createAndAddVehicleTypesForAllEmissionConcepts(Scenario scenario, HbefaVehicleCategory hbefaVehicleCategory){
		VehiclesFactory factory = scenario.getVehicles().getFactory();
		Map<String,VehicleType> emissionConcept2VehicleType = new HashMap<>();
		emissionConcept2VehicleType.put("petrol (4S)", prepareAndAddVehicleType(factory, "petrol_" + hbefaVehicleCategory, hbefaVehicleCategory, "petrol (4S)"));
		emissionConcept2VehicleType.put("diesel", prepareAndAddVehicleType(factory, "diesel_" + hbefaVehicleCategory, hbefaVehicleCategory, "diesel"));
		emissionConcept2VehicleType.put("bifuel CNG/petrol", prepareAndAddVehicleType(factory, "cngHybrid_" + hbefaVehicleCategory, hbefaVehicleCategory,"bifuel CNG/petrol"));
		emissionConcept2VehicleType.put("electricity", prepareAndAddVehicleType(factory, "electric_" + hbefaVehicleCategory,hbefaVehicleCategory, "electricity"));
		emissionConcept2VehicleType.put("Plug-in Hybrid petrol/electric", prepareAndAddVehicleType(factory, "pluginHybridPetrol_" + hbefaVehicleCategory, hbefaVehicleCategory,"Plug-in Hybrid petrol/electric"));
		emissionConcept2VehicleType.put("Plug-in Hybrid diesel/electric", prepareAndAddVehicleType(factory, "pluginHybridDiesel_" + hbefaVehicleCategory, hbefaVehicleCategory,"Plug-in Hybrid diesel/electric"));

		if(hbefaVehicleCategory.equals(HbefaVehicleCategory.PASSENGER_CAR)){
			emissionConcept2VehicleType.put("bifuel LPG/petrol", prepareAndAddVehicleType(factory, "lpgHybrid_" + hbefaVehicleCategory, hbefaVehicleCategory,"bifuel LPG/petrol"));
		}

		//add vehicle types to the scenario
		emissionConcept2VehicleType.values().forEach(vehicleType -> scenario.getVehicles().addVehicleType(vehicleType));

		return emissionConcept2VehicleType;
	}

	private Map<String, VehicleType> createAndAddHeavyGoodsVehicleTypesForAllEmissionConcepts(Scenario scenario){
		VehiclesFactory factory = scenario.getVehicles().getFactory();
		HbefaVehicleCategory hbefaVehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;

		Map<String,VehicleType> emissionConcept2VehicleType = new HashMap<>();
			emissionConcept2VehicleType.put("diesel", prepareAndAddVehicleType(factory, "diesel_" + hbefaVehicleCategory, hbefaVehicleCategory, "diesel"));
			emissionConcept2VehicleType.put("CNG", prepareAndAddVehicleType(factory, "cng_" + hbefaVehicleCategory, hbefaVehicleCategory,"CNG"));
			emissionConcept2VehicleType.put("LNG", prepareAndAddVehicleType(factory, "lng_" + hbefaVehicleCategory, hbefaVehicleCategory,"LNG"));
			emissionConcept2VehicleType.put("electricity", prepareAndAddVehicleType(factory, "electric_" + hbefaVehicleCategory,hbefaVehicleCategory, "electricity"));

		//add vehicle types to the scenario
		emissionConcept2VehicleType.values().forEach(vehicleType -> scenario.getVehicles().addVehicleType(vehicleType));

		return emissionConcept2VehicleType;
	}

	private VehicleType prepareAndAddVehicleType(VehiclesFactory factory, String vehicleTypeId, HbefaVehicleCategory hbefaVehicleCategory, String emissionsConcept) {
		VehicleType vehicleType = factory.createVehicleType(Id.create(vehicleTypeId, VehicleType.class));
		EngineInformation engineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(engineInformation, hbefaVehicleCategory.toString());
		VehicleUtils.setHbefaTechnology(engineInformation, "average");
		VehicleUtils.setHbefaSizeClass(engineInformation, "average");
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, emissionsConcept);
		return vehicleType;
	}

	private void writeOutput(String linkEmissionAnalysisFile, String linkEmissionPerMAnalysisFile, String vehicleTypeFileStr, Scenario scenario, EmissionsOnLinkHandler emissionsEventHandler) throws IOException {

		log.info("Emission analysis completed.");

		log.info("Writing output...");

		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(4);
		nf.setGroupingUsed(false);

		{
			File absolutFile = new File(linkEmissionAnalysisFile);
			File perMeterFile = new File(linkEmissionPerMAnalysisFile);

			BufferedWriter absolutWriter = new BufferedWriter(new FileWriter(absolutFile));
			BufferedWriter perMeterWriter = new BufferedWriter(new FileWriter(perMeterFile));

			absolutWriter.write("linkId");
			perMeterWriter.write("linkId");

			for (Pollutant pollutant : pollutants2Output) {
				absolutWriter.write(";" + pollutant);
				perMeterWriter.write(";" + pollutant + " [g/m]");

			}
			absolutWriter.newLine();
			perMeterWriter.newLine();


			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

			for (Id<Link> linkId : link2pollutants.keySet()) {
				absolutWriter.write(linkId.toString());
				perMeterWriter.write(linkId.toString());

				for (Pollutant pollutant : pollutants2Output) {
					double emissionValue = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emissionValue = link2pollutants.get(linkId).get(pollutant);
					}
					absolutWriter.write(";" + nf.format(emissionValue));

					double emissionPerM = Double.NaN;
					Link link = scenario.getNetwork().getLinks().get(linkId);
					if (link != null) {
						emissionPerM = emissionValue / link.getLength();
					}
					perMeterWriter.write(";" + nf.format(emissionPerM));

				}
				absolutWriter.newLine();
				perMeterWriter.newLine();

			}

			absolutWriter.close();
			log.info("Output written to " + linkEmissionAnalysisFile);
			perMeterWriter.close();
			log.info("Output written to " + linkEmissionPerMAnalysisFile);
		}

		{
			File vehicleTypeFile = new File(vehicleTypeFileStr);

			BufferedWriter vehicleTypeWriter = new BufferedWriter(new FileWriter(vehicleTypeFile));

			vehicleTypeWriter.write("vehicleId;vehicleType;emissionsConcept");
			vehicleTypeWriter.newLine();

			for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
				String emissionsConcept = "null";
				if (vehicle.getType().getEngineInformation() != null && VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) != null) {
					emissionsConcept = VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation());
				}

				vehicleTypeWriter.write(vehicle.getId() + ";" + vehicle.getType().getId().toString() + ";" + emissionsConcept);
				vehicleTypeWriter.newLine();
			}

			vehicleTypeWriter.close();
			log.info("Output written to " + vehicleTypeFileStr);
		}
	}

	private void changeVehicleTypes(Scenario scenario, Map<String, VehicleType> passengerVehicleTypes, Map<String, VehicleType> heavyGoodsVehicleTypes, Map<String, VehicleType> lightCommercialVehicleTypes) {
		VehicleType defaultCarVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class));
		Map<Id<Vehicle>, VehicleType> veh2NewType = new HashMap<>();

		for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {

			// skip transit vehicles
			if (scenario.getTransitVehicles().getVehicles().get(vehicle.getId()) != null) {
				if(! VehicleUtils.getHbefaVehicleCategory( vehicle.getType().getEngineInformation()).equals(HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString())) throw new IllegalStateException();
			} else if (vehicle.getId().toString().contains("freight") || vehicle.getId().toString().contains("commercial") || vehicle.getType().getId().toString().contains("commercial")) {
				if(vehicle.getType().getId().toString().contains("Lkw")){
					String emissionConcept = (String) heavyGoodsVehicleTypeDistribution.sample();
					VehicleType newVehicleType = heavyGoodsVehicleTypes.get(emissionConcept);
					if(newVehicleType == null) throw new IllegalArgumentException("could not find heavy goods vehicleType for emission concept " + emissionConcept);
					veh2NewType.put(vehicle.getId(),newVehicleType);
					continue;
				} else {
					String emissionConcept = (String) lightCommercialVehicleTypeDistribution.sample();
					VehicleType newVehicleType = lightCommercialVehicleTypes.get(emissionConcept);
					if(newVehicleType == null) throw new IllegalArgumentException("could not find light commercial vehicleType for emission concept " + emissionConcept);
					veh2NewType.put(vehicle.getId(),newVehicleType);
					continue;
				}

			} else if (vehicle.getId().toString().contains("drt") || vehicle.getId().toString().contains("taxi") || vehicle.getId().toString().contains("scar")) {
				//drt vehicles are considered to be electric in any case
				//car sharing vehicles as well
				VehicleType newVehicleType = passengerVehicleTypes.get("electricity"); //TODO could also be a light commercial vehicle ?!
				Gbl.assertNotNull(newVehicleType);
				veh2NewType.put(vehicle.getId(),newVehicleType);
				continue;
			} else if (vehicle.getType().getId().toString().equals(defaultCarVehicleType.getId().toString())) {
				String emissionConcept = (String) passengerVehicleTypeDistribution.sample();
				VehicleType newVehicleType = passengerVehicleTypes.get(emissionConcept);
				if(newVehicleType == null) throw new IllegalArgumentException("could not find passenger vehicleType for emission concept " + emissionConcept);
				veh2NewType.put(vehicle.getId(),newVehicleType);
				continue;
			} else {
				throw new RuntimeException("Unknown vehicle type: " + vehicle.getType().getId().toString() + ". Aborting...");
			}
		}

		for (Id<Vehicle> id : veh2NewType.keySet()) {
			scenario.getVehicles().removeVehicle(id);

			Vehicle vehicleNew = scenario.getVehicles().getFactory().createVehicle(id, veh2NewType.get(id));
			scenario.getVehicles().addVehicle(vehicleNew);
//			log.info("Type for vehicle " + id + " changed to: " + veh2NewType.get(id).getId().toString());
		}
	}

	private void prepareNetwork(HamburgExperimentalConfigGroup hamburgCfg, Scenario scenario) {
		// network
		for (Link link : scenario.getNetwork().getLinks().values()) {

			double freespeed = Double.NaN;

			if (link.getFreespeed() <= 25.5 / 3.6) {
				freespeed = link.getFreespeed() * hamburgCfg.getFreeSpeedFactor();
				// for non motorway roads, the free speed level was reduced
			} else {
				freespeed = link.getFreespeed();
				// for motorways, the original speed levels seems ok.
			}

			if(freespeed <= 8.333333333){ //30kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
			} else if(freespeed <= 11.111111111){ //40kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
			} else if(freespeed <= 13.888888889){ //50kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 16.666666667){ //60kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 19.444444444){ //70kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
			} else if(freespeed <= 22.222222222){ //80kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
			} else if(freespeed > 22.222222222){ //faster
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
			} else{
				throw new RuntimeException("Link not considered...");
			}
		}
	}

}


