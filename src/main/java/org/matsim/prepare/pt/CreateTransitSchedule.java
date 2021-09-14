package org.matsim.prepare.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.contrib.gtfs.TransitSchedulePostProcessTools;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.matsim.vehicles.MatsimVehicleWriter;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;

import static org.matsim.run.RunBaseCaseHamburgScenario.VERSION;


/**
 * This script utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 *
 * @author zmeng
 */
@CommandLine.Command(
        name = "transit",
        description = "Create transit schedule from GTFS data",
        showDefaultValues = true
)
public class CreateTransitSchedule implements Callable<Integer> {

    // Todo: Modify file path to 2030 gtfs: Spaces in folder names "HVB-VEP_Szenario-1_20210720 GTFS_210826" may lead to errors
    @CommandLine.Option(names = "--input-gtfs", description = "Input GTFS zip files",required = true,defaultValue = "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/realLabHH/data/ReallabHH2030_PT_2030/v2_groesserer_Ausschnitt/Ready4matsim_HVB-VEP_Szenario-1_20210720_GTFS_210826.zip")
//    @CommandLine.Option(names = "--input-gtfs", description = "Input GTFS zip files",required = true,defaultValue = "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/realLabHH/data/gtfs_2019/Upload__HVV_Rohdaten_GTFS_Fpl_20200810.zip")
    private List<Path> gtfsFiles;

//    @CommandLine.Option(names = "--network", description = "Base network that will be merged with pt network.", required = true, defaultValue = "scenarios/input/hamburg-" + VERSION + "-network.xml.gz")
    @CommandLine.Option(names = "--network", description = "Base network that will be merged with pt network.", required = true, defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.0/hamburg-v1.0-network.xml.gz")
    private Path networkFile;

    @CommandLine.Option(names = "--output", description = "Output folder", defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input")
    private File output;

    @CommandLine.Option(names = "--input-cs", description = "Input coordinate system of the data", defaultValue = TransformationFactory.WGS84)
    private String inputCS;

    @CommandLine.Option(names = "--target-cs", description = "Target coordinate system of the network", defaultValue = RunBaseCaseHamburgScenario.COORDINATE_SYSTEM)
    private String targetCS;

//    @CommandLine.Option(names = "--date", description = "The day for which the schedules will be extracted", defaultValue = "2020-09-09")
    @CommandLine.Option(names = "--date", description = "The day for which the schedules will be extracted", defaultValue = "2021-08-26")
    private LocalDate date;

    public static void main(String[] args) {
        System.exit(new CommandLine(new CreateTransitSchedule()).execute(args));
    }

    @Override
    public Integer call(){

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCS, targetCS);

        // Output files
        File scheduleFile = new File(output+"/pt", "hamburg-" + VERSION + "-transitSchedule.xml.gz");
        File networkPTFile = new File(output+"/network", networkFile.toString().replace(".xml", "-with-pt.xml"));
        File transitVehiclesFile = new File(output+"/pt", "hamburg-" + VERSION + "-transitVehicles.xml.gz");

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        for (Path gtfsFile : gtfsFiles) {

            GtfsConverter converter = GtfsConverter.newBuilder()
                    .setScenario(scenario)
                    .setTransform(ct)
                    .setDate(date)
                    .setFeed(gtfsFile)
                    .setMergeStops(true)
                    .build();

            converter.convert();
        }

        //TODO : should we copy?
        TransitSchedulePostProcessTools.copyLateDeparturesToStartOfDay(scenario.getTransitSchedule(), 86400.0D, "copied", false);
        TransitSchedulePostProcessTools.copyEarlyDeparturesToFollowingNight(scenario.getTransitSchedule(), 21600.0D, "copied");

        Network network = Files.exists(networkFile) ? NetworkUtils.readNetwork(networkFile.toString()) : scenario.getNetwork();

        // Create a network around the schedule
        new CreatePseudoNetwork(scenario.getTransitSchedule(), network, "pt_").createNetwork();
        new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleFile.getAbsolutePath());
        new NetworkWriter(network).write(networkPTFile.getAbsolutePath());
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(transitVehiclesFile.getAbsolutePath());

        return 0;
    }
}
