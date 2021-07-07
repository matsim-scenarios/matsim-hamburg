package org.matsim.analysis;
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author ikaddoura
 */

public class EmissionAnalysis {
    private static final Logger log = Logger.getLogger(EmissionAnalysis.class);

    public static void main(String[] args) throws IOException {

        if ( args.length==0 ) {
            args = new String[] {"scenarios/berlin-v5.5-1pct.config.xml"};
        }

        Config config = ConfigUtils.loadConfig(args, new AnalysisConfigGroup(), new EmissionsConfigGroup());

        AnalysisConfigGroup aConfigGroup = (AnalysisConfigGroup) config.getModules().get(AnalysisConfigGroup.GROUP_NAME);

        config.vehicles().setVehiclesFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_vehicles.xml.gz");
        config.network().setInputFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_network.xml.gz");
        config.transit().setTransitScheduleFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_transitSchedule.xml.gz");
        config.transit().setVehiclesFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_transitVehicles.xml.gz");
        config.global().setCoordinateSystem("GK4");
        config.plans().setInputFile(null);
        config.parallelEventHandling().setNumberOfThreads(null);
        config.parallelEventHandling().setEstimatedNumberOfEvents(null);
        config.global().setNumberOfThreads(1);

        EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort);
        eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
        eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);

        String analysisOutputDirectory = config.controler().getOutputDirectory();
        if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";

        File folder = new File(analysisOutputDirectory);
        folder.mkdirs();

        final String emissionEventOutputFile = analysisOutputDirectory + aConfigGroup.getRunId() + ".emission.events.offline.xml.gz";
        final String eventsFile = aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_events.xml.gz";

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // network
        for (Link link : scenario.getNetwork().getLinks().values()) {

            double freespeed = Double.NaN;

            if (link.getFreespeed() <= 13.888889) {
                freespeed = link.getFreespeed() * 2;
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

        // car vehicles

        VehicleType petrolCarVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("petrolCar", VehicleType.class));
        scenario.getVehicles().addVehicleType(petrolCarVehicleType);
        EngineInformation petrolCarEngineInformation = petrolCarVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( petrolCarEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( petrolCarEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( petrolCarEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( petrolCarEngineInformation, "petrol (4S)" );

        VehicleType dieselCarVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("dieselCar", VehicleType.class));
        scenario.getVehicles().addVehicleType(dieselCarVehicleType);
        EngineInformation dieselCarEngineInformation = dieselCarVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( dieselCarEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( dieselCarEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( dieselCarEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( dieselCarEngineInformation, "diesel" );

        VehicleType cngVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("cngCar", VehicleType.class));
        scenario.getVehicles().addVehicleType(cngVehicleType);
        EngineInformation cngCarEngineInformation = cngVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( cngCarEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( cngCarEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( cngCarEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( cngCarEngineInformation, "bifuel CNG/petrol" );

        VehicleType lpgVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("lpgCar", VehicleType.class));
        scenario.getVehicles().addVehicleType(lpgVehicleType);
        EngineInformation lpgCarEngineInformation = lpgVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( lpgCarEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( lpgCarEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( lpgCarEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( lpgCarEngineInformation, "bifuel LPG/petrol" );

        // electric vehicles
        VehicleType electricVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("electricCar", VehicleType.class));
        scenario.getVehicles().addVehicleType(electricVehicleType);
        EngineInformation electricEngineInformation = electricVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( electricEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( electricEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( electricEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( electricEngineInformation, "electricity" );

        // plug-in hybrid petrol vehicles
        VehicleType pluginHybridPetrolVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("pluginHybridPetrol", VehicleType.class));
        scenario.getVehicles().addVehicleType(pluginHybridPetrolVehicleType);
        EngineInformation pluginHybridPetrolEngineInformation = pluginHybridPetrolVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( pluginHybridPetrolEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( pluginHybridPetrolEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( pluginHybridPetrolEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( pluginHybridPetrolEngineInformation, "Plug-in Hybrid petrol/electric" );

        // plug-in hybrid petrol vehicles
        VehicleType pluginHybridDieselVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("pluginHybridDiesel", VehicleType.class));
        scenario.getVehicles().addVehicleType(pluginHybridDieselVehicleType);
        EngineInformation pluginHybridDieselEngineInformation = pluginHybridDieselVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( pluginHybridDieselEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology( pluginHybridDieselEngineInformation, "average" );
        VehicleUtils.setHbefaSizeClass( pluginHybridDieselEngineInformation, "average" );
        VehicleUtils.setHbefaEmissionsConcept( pluginHybridDieselEngineInformation, "Plug-in Hybrid diesel/electric" );

        // ignore default car vehicles

        VehicleType defaultCarVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("car", VehicleType.class));
        EngineInformation carEngineInformation = defaultCarVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());

        // ignore freight vehicles

        VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create("freight", VehicleType.class));
        EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());

        // ignore public transit vehicles

        for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
            EngineInformation engineInformation = type.getEngineInformation();
            VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
        }

        List<Id<Vehicle>> vehiclesToChangeToElectric = new ArrayList<>();
        List<Id<Vehicle>> carVehiclesToChangeToSpecificType = new ArrayList<>();

        final Random rnd = MatsimRandom.getLocalInstance();

        int totalVehiclesCounter = 0;
        // randomly change some vehicle types
        for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
            totalVehiclesCounter++;
            if (vehicle.getId().toString().contains("freight")) {
                // some freight vehicles have the type "car", skip them...

            } else if (vehicle.getType().getId().toString().equals(defaultCarVehicleType.getId().toString())) {

                carVehiclesToChangeToSpecificType.add(vehicle.getId());

                if (rnd.nextDouble() < aConfigGroup.getElectricVehicleShare()) {
                    vehiclesToChangeToElectric.add(vehicle.getId());
                }
            } else {
                // ignore all other vehicles
            }
        }

        final double petrolShare = 0.512744724750519;
        final double dieselShare = 0.462841421365738;
        final double lpgShare = 0.011381645;
        final double cngShare = 0.0038579236716032;
        final double hybridPetrolShare = 0.005743607878685;
        final double hybridDieselShare = 0.00014232617104426;

        for (Id<Vehicle> id : carVehiclesToChangeToSpecificType) {
            scenario.getVehicles().removeVehicle(id);

            VehicleType vehicleType;
            double rndNumber = rnd.nextDouble();
            if (rndNumber < petrolShare) {
                // petrol
                vehicleType = petrolCarVehicleType;
            } else if (rndNumber >= petrolShare && rndNumber < petrolShare + dieselShare) {
                // diesel
                vehicleType = dieselCarVehicleType;
            } else if (rndNumber >= petrolShare + dieselShare && rndNumber < petrolShare + dieselShare + lpgShare) {
                // lpg
                vehicleType = lpgVehicleType;
            } else if (rndNumber >= petrolShare + dieselShare + lpgShare && rndNumber < petrolShare + dieselShare + lpgShare + cngShare) {
                // cng
                vehicleType = cngVehicleType;
            } else if (rndNumber >= petrolShare + dieselShare + lpgShare + cngShare && rndNumber < petrolShare + dieselShare + lpgShare + cngShare + hybridPetrolShare) {
                // hybrid petrol
                vehicleType = pluginHybridPetrolVehicleType;
            } else if (rndNumber >= petrolShare + dieselShare + lpgShare + cngShare + hybridPetrolShare && rndNumber < petrolShare + dieselShare + lpgShare + cngShare + hybridPetrolShare + hybridDieselShare) {
                // hybrid diesel
                vehicleType = pluginHybridDieselVehicleType;
            } else {
                // electric
                vehicleType = electricVehicleType;
            }

            Vehicle vehicleNew = scenario.getVehicles().getFactory().createVehicle(id, vehicleType);
            scenario.getVehicles().addVehicle(vehicleNew);
            log.info("Type for vehicle " + id + " changed to: " + vehicleType.getId().toString());
        }

        for (Id<Vehicle> id : vehiclesToChangeToElectric) {
            scenario.getVehicles().removeVehicle(id);
            Vehicle vehicleNew = scenario.getVehicles().getFactory().createVehicle(id, electricVehicleType);
            scenario.getVehicles().addVehicle(vehicleNew);
            log.info("Type for vehicle " + id + " changed to electric.");
        }

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

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        EmissionsOnLinkHandler emissionsEventHandler = new EmissionsOnLinkHandler();
        eventsManager.addHandler(emissionsEventHandler);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();

        log.info("Total number of vehicles: " + totalVehiclesCounter);
        log.info("Number of passenger car vehicles: " + carVehiclesToChangeToSpecificType.size());
        log.info("Number of passenger car vehicles that are changed to electric vehicles: " + vehiclesToChangeToElectric.size());

        log.info("Emission analysis completed.");

        log.info("Writing output...");

        {
            String fileName = analysisOutputDirectory + aConfigGroup.getRunId() + ".emissionsPerLink.csv";
            File file1 = new File(fileName);

            BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

            bw1.write("linkId");

            for (Pollutant pollutant : Pollutant.values()) {
                bw1.write(";" + pollutant);
            }
            bw1.newLine();

            Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

            for (Id<Link> linkId : link2pollutants.keySet()) {
                bw1.write(linkId.toString());

                for (Pollutant pollutant : Pollutant.values()) {
                    double value = 0.;
                    if (link2pollutants.get(linkId).get(pollutant) != null) {
                        value = link2pollutants.get(linkId).get(pollutant);
                    }
                    bw1.write(";" + value);
                }
                bw1.newLine();
            }

            bw1.close();
            log.info("Output written to " + fileName);
        }

        {
            String fileName2 = analysisOutputDirectory + aConfigGroup.getRunId() + ".vehicleTypes.csv";
            File file2 = new File(fileName2);

            BufferedWriter bw2 = new BufferedWriter(new FileWriter(file2));

            bw2.write("vehicleId;vehicleType;emissionsConcept");
            bw2.newLine();

            for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
                String emissionsConcept = "null";
                if (vehicle.getType().getEngineInformation() != null && VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) != null) {
                    emissionsConcept = VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()).toString();
                }

                bw2.write(vehicle.getId() + ";" + vehicle.getType().getId().toString() + ";" + emissionsConcept);
                bw2.newLine();
            }

            bw2.close();
            log.info("Output written to " + fileName2);
        }

    }

}
