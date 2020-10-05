package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.RunBaseCaseHamburgScenario;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import static org.matsim.run.RunBaseCaseHamburgScenario.VERSION;
/**
 * Creates the road network layer.
 *
 * use https://download.geofabrik.de/europe/germany/hamburg-latest.osm.pbf
 * @author zmeng
 */

@CommandLine.Command(
        name = "network",
        description = "Create MATSim network from OSM data",
        showDefaultValues = true
)
public class CreateNetwork implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(CreateNetwork.class);

    @CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT", description = "Input file", defaultValue = "/Users/meng/work/realLabHH/files/germany-latest.osm.pbf")
    private List<Path> input;

    @CommandLine.Option(names = "--from-osm", description = "Import from OSM without lane information", defaultValue = "false")
    private boolean fromOSM;

    @CommandLine.Option(names = "--output", description = "Output xml file", defaultValue = "scenarios/input/hamburg-" + VERSION + "-network.xml.gz")
    private File output;


    public static void main(String[] args) {
        args = new String[] {"--from-osm"};
        System.exit(new CommandLine(new CreateNetwork()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        if (fromOSM) {

            CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);

            Network network = new SupersonicOsmNetworkReader.Builder()
                    .setCoordinateTransformation(ct)
                    .setIncludeLinkAtCoordWithHierarchy((coord, hierachyLevel) ->
                                    hierachyLevel <= LinkProperties.LEVEL_RESIDENTIAL &&
                                            coord.getX() >= (RunBaseCaseHamburgScenario.X_EXTENT[0] - 50000) && (coord.getX() <= RunBaseCaseHamburgScenario.X_EXTENT[1] + 50000) &&
                                            coord.getY() >= (RunBaseCaseHamburgScenario.Y_EXTENT[0] - 50000) && (coord.getY() <= RunBaseCaseHamburgScenario.Y_EXTENT[1] + 50000)
                    )

                    .setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.bike, TransportMode.ride))))
                    .build()
                    .read(input.get(0));

            NetworkUtils.runNetworkCleaner(network);
            new NetworkWriter(network).write(output.getAbsolutePath());

            return 0;
        }
        return 0;
    }
}
