package org.matsim.prepare;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectReferencePair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.contrib.sumo.SumoNetworkConverter;
import org.matsim.contrib.sumo.SumoNetworkHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.lanes.*;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.matsim.utils.objectattributes.attributable.Attributable;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Creates the road network layer.
 * <p>
 * Use https://download.geofabrik.de/europe/germany/nordrhein-westfalen/duesseldorf-regbez-latest.osm.pbf
 *
 * @author rakow
 */
@CommandLine.Command(
		name = "network",
		description = "Create MATSim network from OSM data",
		showDefaultValues = true
)
public final class CreateNetwork implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateNetwork.class);

	/**
	 * Capacities below this threshold are unplausible and ignored.
	 */
	private static final double CAPACITY_THRESHOLD = 375;

	@CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT", description = "Input file", defaultValue = "scenarios/input/sumo.net.xml")
	private List<Path> input;

	@CommandLine.Option(names = "--output", description = "Output xml file", defaultValue = "scenarios/input/hamburg-" + RunBaseCaseHamburgScenario.VERSION + "-network.xml.gz")
	private Path output;

	@CommandLine.Option(names = "--from-osm", description = "Import from OSM without lane information", defaultValue = "false")
	private boolean fromOSM;

	@CommandLine.Option(names = {"--capacities"}, description = "CSV file with lane capacities", required = false)
	private Path capacities;

	public static void main(String[] args) {
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
									coord.getX() >= RunBaseCaseHamburgScenario.X_EXTENT[0] && coord.getX() <= RunBaseCaseHamburgScenario.X_EXTENT[1] &&
									coord.getY() >= RunBaseCaseHamburgScenario.Y_EXTENT[0] && coord.getY() <= RunBaseCaseHamburgScenario.Y_EXTENT[1]
					)

					.setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.bike, TransportMode.ride))))
					.build()
					.read(input.get(0));

			new NetworkWriter(network).write(output.toAbsolutePath().toString());

			return 0;
		}

		SumoNetworkConverter converter = SumoNetworkConverter.newInstance(input, output, null, "EPSG:32632", RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);

		Network network = NetworkUtils.createNetwork();
		Lanes lanes = LanesUtils.createLanesContainer();

		SumoNetworkHandler handler = converter.convert(network, lanes);

		converter.calculateLaneCapacities(network, lanes);

		// This needs to run without errors, otherwise network is broken
		network.getLinks().values().forEach(link -> {
			LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
			if (l2l != null)
				LanesUtils.createLanes(link, l2l);
		});

		if (capacities != null) {

			Object2DoubleMap<Triple<Id<Link>, Id<Link>, Id<Lane>>> map = readLaneCapacities(capacities);

			log.info("Read lane capacities from {}, containing {} lanes", capacities, map.size());

			int n = setLinkCapacities(network, map);
			int n2 = setLaneCapacities(lanes, map);

			log.info("Unmatched links: {}, lanes: {}", n, n2);

		}

		new NetworkWriter(network).write(output.toAbsolutePath().toString());
		new LanesWriter(lanes).write(output.toAbsolutePath().toString().replace(".xml", "-lanes.xml"));

		converter.writeGeometry(handler, output.toAbsolutePath().toString().replace(".xml", "-linkGeometries.csv").replace(".gz", ""));

		return 0;
	}

	/**
	 * Read lane capacities from csv file.
	 *
	 * @return triples of fromLink, toLink, fromLane
	 */
	public static Object2DoubleMap<Triple<Id<Link>, Id<Link>, Id<Lane>>> readLaneCapacities(Path input) {

		Object2DoubleMap<Triple<Id<Link>, Id<Link>, Id<Lane>>> result = new Object2DoubleOpenHashMap<>();

		try (CSVParser parser = new CSVParser(IOUtils.getBufferedReader(input.toString()),
				CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {

				Id<Link> fromLinkId = Id.create(record.get("fromEdgeId"), Link.class);
				Id<Link> toLinkId = Id.create(record.get("toEdgeId"), Link.class);
				Id<Lane> fromLaneId = Id.create(record.get("fromLaneId"), Lane.class);

				Triple<Id<Link>, Id<Link>, Id<Lane>> key = Triple.of(fromLinkId, toLinkId, fromLaneId);

				result.mergeDouble(key, Integer.parseInt(record.get("intervalVehicleSum")), Double::sum);

			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return result;
	}

	/**
	 * Aggregate maximum lane capacities, independent of turning direction.
	 */
	public static Object2DoubleMap<Pair<Id<Link>, Id<Lane>>> calcMaxLaneCapacities(Object2DoubleMap<Triple<Id<Link>, Id<Link>, Id<Lane>>> map) {

		Object2DoubleMap<Pair<Id<Link>, Id<Lane>>> laneCapacities = new Object2DoubleOpenHashMap<>();

		// sum for each link
		for (Object2DoubleMap.Entry<Triple<Id<Link>, Id<Link>, Id<Lane>>> e : map.object2DoubleEntrySet()) {
			laneCapacities.mergeDouble(ObjectReferencePair.of(e.getKey().getLeft(), e.getKey().getRight()), e.getDoubleValue(), Double::max);
		}

		return laneCapacities;
	}

	/**
	 * Use provided lane capacities, to calculate aggregated capacities for all links.
	 * This does not modify lane capacities.
	 *
	 * @return number of links from file that are not in the network.
	 */
	public static int setLinkCapacities(Network network, Object2DoubleMap<Triple<Id<Link>, Id<Link>, Id<Lane>>> map) {

		Object2DoubleMap<Id<Link>> linkCapacities = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<Pair<Id<Link>, Id<Lane>>> laneCapacities = calcMaxLaneCapacities(map);

		// sum for each link
		for (Object2DoubleMap.Entry<Pair<Id<Link>, Id<Lane>>> e : laneCapacities.object2DoubleEntrySet()) {
			linkCapacities.mergeDouble(e.getKey().key(), e.getDoubleValue(), Double::sum);
		}

		int unmatched = 0;

		for (Object2DoubleMap.Entry<Id<Link>> e : linkCapacities.object2DoubleEntrySet()) {

			Link link = network.getLinks().get(e.getKey());

			if (link != null) {
				// ignore unplausible capacities
				if (e.getDoubleValue() < CAPACITY_THRESHOLD * link.getNumberOfLanes())
					continue;

				link.setCapacity(e.getDoubleValue());
				link.getAttributes().putAttribute("junction", true);
			} else {
				unmatched++;
			}
		}

		Object2DoubleMap<Pair<Id<Link>, Id<Link>>> turnCapacities = new Object2DoubleOpenHashMap<>();

		for (Object2DoubleMap.Entry<Triple<Id<Link>, Id<Link>, Id<Lane>>> e : map.object2DoubleEntrySet()) {
			turnCapacities.mergeDouble(Pair.of(e.getKey().getLeft(), e.getKey().getMiddle()), e.getDoubleValue(), Double::sum);
		}

		// set turn capacities relative to whole link capacity
		for (Object2DoubleMap.Entry<Pair<Id<Link>, Id<Link>>> e : turnCapacities.object2DoubleEntrySet()) {

			Id<Link> fromLink = e.getKey().left();
			Id<Link> toLink = e.getKey().right();

			Link link = network.getLinks().get(fromLink);

			if (link == null)
				continue;

			getTurnEfficiencyMap(link).put(toLink.toString(), String.valueOf(e.getDoubleValue() / link.getCapacity()));
		}


		return unmatched;
	}

	/**
	 * Use provided lane capacities and apply them in the network.
	 *
	 * @return number of lanes in file, but not in the network.
	 */
	public static int setLaneCapacities(Lanes lanes, Object2DoubleMap<Triple<Id<Link>, Id<Link>, Id<Lane>>> map) {

		Object2DoubleMap<Pair<Id<Link>, Id<Lane>>> laneCapacities = calcMaxLaneCapacities(map);

		int unmatched = 0;

		SortedMap<Id<Link>, LanesToLinkAssignment> l2ls = lanes.getLanesToLinkAssignments();

		for (Object2DoubleMap.Entry<Pair<Id<Link>, Id<Lane>>> e : laneCapacities.object2DoubleEntrySet()) {

			LanesToLinkAssignment l2l = l2ls.get(e.getKey().key());

			if (l2l == null) {
				unmatched++;
				continue;
			}

			Lane lane = l2l.getLanes().get(e.getKey().right());

			if (lane == null) {
				unmatched++;
				continue;
			}

			// ignore unplausible capacities
			if (e.getDoubleValue() < CAPACITY_THRESHOLD)
				continue;

			lane.setCapacityVehiclesPerHour(e.getDoubleValue());
		}

		// set turn efficiency depending on to link
		for (Object2DoubleMap.Entry<Triple<Id<Link>, Id<Link>, Id<Lane>>> e : map.object2DoubleEntrySet()) {

			LanesToLinkAssignment l2l = l2ls.get(e.getKey().getLeft());
			if (l2l == null) continue;

			Lane lane = l2l.getLanes().get(e.getKey().getRight());
			if (lane == null) continue;

			Id<Link> toLink = e.getKey().getMiddle();
			getTurnEfficiencyMap(lane).put(toLink.toString(), String.valueOf(e.getDoubleValue() / lane.getCapacityVehiclesPerHour()));
		}


		return unmatched;
	}

	/**
	 * Retrieves turn efficiency from attributes.
	 */
	private static Map<String, String> getTurnEfficiencyMap(Attributable obj) {
		Map<String, String> cap = (Map<String, String>) obj.getAttributes().getAttribute("turnEfficiency");
		if (cap == null) {
			cap = new HashMap<>();
			obj.getAttributes().putAttribute("v", cap);
		}

		return cap;
	}

}
