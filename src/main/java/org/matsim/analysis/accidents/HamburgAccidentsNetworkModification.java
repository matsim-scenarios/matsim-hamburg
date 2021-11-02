package org.matsim.analysis.accidents;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author mmayobre, ikaddoura, tschlenther
 *
 *  copied from accidents contrib
 */

public class HamburgAccidentsNetworkModification {
    private static final Logger log = Logger.getLogger(HamburgAccidentsNetworkModification.class);

    private final Scenario scenario;

    @Deprecated //use static method
    HamburgAccidentsNetworkModification(Scenario scenario) {
        this.scenario = scenario;
    }

    public static void main(String[] args) {

        AccidentsConfigGroup accidentsSettings = new AccidentsConfigGroup();
        Network network = NetworkUtils.readTimeInvariantNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-with-pt.xml.gz");

        Network networkWithRealisticNumberOfLanes = NetworkUtils.readTimeInvariantNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.0/hamburg-v1.0-network-with-pt.xml.gz");

        Set<Id<Link>> tunnelLinks = readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_tunnel_2021.csv");
        Set<Id<Link>> planfreeLinks = readCSVFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg_hvv_planfree_2021.csv");

        HamburgAccidentsNetworkModification.setLinkAttributesBasedOnInTownShapeFile(accidentsSettings,
                network,
                networkWithRealisticNumberOfLanes,
                "D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/shp/innerorts-ausserorts/hamburg_hvv_innerorts_inkl_HH_city_reduced.shp",
                tunnelLinks,
                planfreeLinks);

        NetworkUtils.writeNetwork( network,"D:/svn/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/baseCase/input/hamburg-v2.0-network-with-pt-with-accidentAttributes.xml.gz");

    }

    /**
     *
     * @param accidentsConfigGroup
     * @param network
     * @param networkToLookupLanes
     * @param pathToInTownShapeFile
     * @param tunnelLinkIds
     * @param planfreeLinkIds
     */
    static void setLinkAttributesBasedOnInTownShapeFile(AccidentsConfigGroup accidentsConfigGroup, Network network,
                                                        Network networkToLookupLanes, String pathToInTownShapeFile,
                                                        Set<Id<Link>> tunnelLinkIds, Set<Id<Link>> planfreeLinkIds){
        log.info("Initializing all link-specific information...");

        List<PreparedGeometry> inTownShapes = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(pathToInTownShapeFile));

        Counter counter = new Counter("link # ");
        for (Link link : network.getLinks().values()) {
            counter.incCounter();

            link.getAttributes().putAttribute(accidentsConfigGroup.getAccidentsComputationMethodAttributeName(), AccidentsConfigGroup.AccidentsComputationMethod.BVWP.toString());

            // 'plangleich', 'planfrei' or tunnel?
            int junctionType = 1; //default plangleich
            // builtup or not builtup area? and Kraftfahrstrasse or not Kraftfahrstrasse?
            int areaAndRoadType = 0; // default = outside of town/not built-up area and no kfz straße
            //number of lanes
            int lanes;

            Set<String> modes = link.getAllowedModes();
            if(  modes.contains(TransportMode.pt) && !modes.contains(TransportMode.car) ){
                //IMPORTANT: we ignore accident costs of pure pt links. So far, it is unclear whether the link is a street or a railway, for which the cost rates differ by a factor of 10!
                //so far, pt links have not been handled separately at all! As we have varying pt schedules with varying model areas in between the scenarios, i chose to ignore pt incidents cost for the first approach!
                //for a more sophisticated version, one could look up the line type from the pt schedule and distinguish railway pt lines and bus lines.

                junctionType = 0; //plan free
                areaAndRoadType = 0; //not built-up area, no kfz-straße
                lanes = 1;

            } else {
                //car is allowed. this link seems to model a street/roadway.

                if(planfreeLinkIds.contains(link.getId())){
                    junctionType = 0;
                }
                if(tunnelLinkIds.contains(link.getId())){ //if both plan free and tunnel then use tunnel
                    junctionType = 2;
                }

                //consider middle of the link
                if(ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), inTownShapes)){
                    areaAndRoadType += 1;
                }

                //if more than 16 m/s then we model it as kraftfahrstrasse, which have indices 0 and 1 (2 and 3 are for not kraftfahrstrasse)
                if(link.getFreespeed() < 16.){
                    areaAndRoadType += 2;
                }

                Link lanesLink;
                if(!networkToLookupLanes.getLinks().containsKey(link.getId())){
                    log.warn("could not find link " + link.getId() + " in the lane-lookup network. will use the original link");
                    lanesLink = link;
                } else {
                    lanesLink = networkToLookupLanes.getLinks().get(link.getId());
                }

                if (lanesLink.getNumberOfLanes() > 4.0){
                    log.warn("Set number of lanes to 4");
                    lanes = 4;
                }
                else if(lanesLink.getNumberOfLanes()<1.0) {
                    lanes = 1;
                }
                else {
                    lanes = (int) lanesLink.getNumberOfLanes();
                }
            }
            link.getAttributes().putAttribute( AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME, junctionType + "," + areaAndRoadType + "," + lanes);
        }
        log.info("Initializing all link-specific information... Done.");
    }

    static Set<Id<Link>> readCSVFile(String csvFile) {
        Set<Id<Link>> links = new HashSet<>();

        BufferedReader br = IOUtils.getBufferedReader(csvFile);

        String line = null;
        try {
            line = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            int countWarning = 0;
            while ((line = br.readLine()) != null) {

                String[] columns = line.split(";");
                Id<Link> linkId = null;
                for (int column = 0; column < columns.length; column++) {
                    if (column == 0) {
                        linkId = Id.createLinkId(columns[column]);
                    } else {
                        if (countWarning < 1) {
                            log.warn("Expecting the link Id to be in the first column. Ignoring further columns...");
                        } else if (countWarning == 1) {
                            log.warn("This message is only given once.");
                        }
                        countWarning++;
                    }
                }
                log.info("Adding link ID " + linkId);
                links.add(linkId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    @Deprecated //this was copied from elsewhere (contrib or matsim-berlin)
    Network setLinkAttributesBasedOnOSMFile(String landuseOsmFile, String osmCRS, String[] tunnelLinkIDs, String[] planfreeLinkIDs) throws IOException {

        AccidentsConfigGroup accidentsCfg = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);

        Map<String, SimpleFeature> landUseFeaturesBB = new HashMap<>();
        Map<String, String> landUseDataBB = new HashMap<>();


        log.info("Initializing all link-specific information...");

        if (landuseOsmFile == null) {
            log.warn("Landuse shape file is null. Using default values...");
        } else {
            SimpleFeatureSource ftsLandUseBB;
            if (!landuseOsmFile.startsWith("http")) {
                ftsLandUseBB = ShapeFileReader.readDataFile(landuseOsmFile);
            } else {
                ftsLandUseBB = FileDataStoreFinder.getDataStore(new URL(landuseOsmFile)).getFeatureSource();
            }
            try (SimpleFeatureIterator itLandUseBB = ftsLandUseBB.getFeatures().features()) {
                while (itLandUseBB.hasNext()) {
                    SimpleFeature ftLandUseBB = itLandUseBB.next();
                    String osmId = ftLandUseBB.getAttribute("osm_id").toString();
                    String fclassName = ftLandUseBB.getAttribute("fclass").toString();
                    landUseFeaturesBB.put(osmId, ftLandUseBB);
                    landUseDataBB.put(osmId, fclassName);
                }
                itLandUseBB.close();
                DataStore ds = (DataStore) ftsLandUseBB.getDataStore();
                ds.dispose();
                log.info("Reading shp file for built-up/nonbuilt-up area & AreaType... Done.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        int linkCounter = 0;
        for (Link link : this.scenario.getNetwork().getLinks().values()) {

            if (linkCounter % 100 == 0) {
                log.info("Link #" + linkCounter + "  (" + (int) ((double) linkCounter / this.scenario.getNetwork().getLinks().size() * 100) + "%)");
            }
            linkCounter++;

            link.getAttributes().putAttribute(accidentsCfg.getAccidentsComputationMethodAttributeName(), AccidentsConfigGroup.AccidentsComputationMethod.BVWP.toString());

            ArrayList<Integer> bvwpRoadType = new ArrayList<>();

            // 'plangleich', 'planfrei' or tunnel?
            bvwpRoadType.add(0, 1);

            if (planfreeLinkIDs != null) {
                for(int j=0; j < planfreeLinkIDs.length; j++){
                    if(planfreeLinkIDs[j].equals(String.valueOf(link.getId()))){
                        bvwpRoadType.set(0, 0); // Change to Plan free
                        log.info(link.getId() + " Changed to Plan free!");
                        break;
                    }
                }
            }

            if (tunnelLinkIDs != null) {
                for(int i=0; i < tunnelLinkIDs.length; i++){
                    if(tunnelLinkIDs[i].equals(String.valueOf(link.getId()))){
                        bvwpRoadType.set(0, 2); // Change to Tunnel
                        log.info(link.getId() + " Changed to Tunnel!");
                        break;
                    }
                }
            }

            // builtup or not builtup area?
            String osmLandUseFeatureBBId = getOSMLandUseFeatureBBId(link, landUseFeaturesBB, osmCRS);

            if (osmLandUseFeatureBBId == null) {
                log.warn("No area type found for link " + link.getId() + ". Using default value: not built-up area.");
                if (link.getFreespeed() > 16.) {
                    bvwpRoadType.add(1, 0);
                } else {
                    bvwpRoadType.add(1, 2);
                }

            } else {
                String landUseTypeBB = landUseDataBB.get(osmLandUseFeatureBBId);
                if (landUseTypeBB.matches("commercial|industrial|recreation_ground|residential|retail")) { //built-up area
                    if (link.getFreespeed() > 16.) {
                        bvwpRoadType.add(1, 1);
                    } else {
                        bvwpRoadType.add(1, 3);
                    }
                } else {
                    if (link.getFreespeed() > 16.) {
                        bvwpRoadType.add(1, 0);
                    } else {
                        bvwpRoadType.add(1, 2);
                    }
                }
            }

            int numberOfLanesBVWP;
            if (link.getNumberOfLanes() > 4.0){
                log.warn("Set number of lanes to 4");
                numberOfLanesBVWP = 4;
            }
            else if(link.getNumberOfLanes()<1.0) {
                numberOfLanesBVWP = 1;
            }
            else {
                numberOfLanesBVWP = (int) link.getNumberOfLanes();
            }
            bvwpRoadType.add(2, numberOfLanesBVWP);

            link.getAttributes().putAttribute( AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME, bvwpRoadType.get(0) + "," + bvwpRoadType.get(1) + "," + bvwpRoadType.get(2));
        }
        log.info("Initializing all link-specific information... Done.");
        return scenario.getNetwork();
    }

    @Deprecated
    private String getOSMLandUseFeatureBBId(Link link, Map<String, SimpleFeature> landUseFeaturesBB, String osmCRS) {

        if (landUseFeaturesBB == null || landUseFeaturesBB.isEmpty()) return null;

        CoordinateTransformation ctScenarioCRS2osmCRS = TransformationFactory.getCoordinateTransformation(this.scenario.getConfig().global().getCoordinateSystem(), osmCRS);

        Coord linkCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getCoord()); // this Method gives the middle point of the link back
        Point pMiddle = MGC.xy2Point(linkCoordinateTransformedToOSMCRS.getX(), linkCoordinateTransformedToOSMCRS.getY());

        Coord linkStartCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getFromNode().getCoord());
        Point pStart = MGC.xy2Point(linkStartCoordinateTransformedToOSMCRS.getX(), linkStartCoordinateTransformedToOSMCRS.getY());

        Coord linkEndCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getToNode().getCoord());
        Point pEnd = MGC.xy2Point(linkEndCoordinateTransformedToOSMCRS.getX(), linkEndCoordinateTransformedToOSMCRS.getY());

        String osmLandUseFeatureBBId = null;

        for (SimpleFeature feature : landUseFeaturesBB.values()) {
            if (((Geometry) feature.getDefaultGeometry()).contains(pMiddle)) {
                return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
            }
        }

        for (SimpleFeature feature : landUseFeaturesBB.values()) {
            if (((Geometry) feature.getDefaultGeometry()).contains(pStart)) {
                return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
            }
        }

        for (SimpleFeature feature : landUseFeaturesBB.values()) {
            if (((Geometry) feature.getDefaultGeometry()).contains(pEnd)) {
                return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
            }
        }

        // look around the link

        GeometryFactory geoFac = new GeometryFactory();
        CoordinateTransformation cTosmCRSToGK4 = TransformationFactory.getCoordinateTransformation(osmCRS, "EPSG:31468");

        double distance = 10.0;

        while (osmLandUseFeatureBBId == null && distance <= 500) {
            Coord coordGK4 = cTosmCRSToGK4.transform(MGC.coordinate2Coord(pMiddle.getCoordinate()));
            Point pGK4 = geoFac.createPoint(MGC.coord2Coordinate(coordGK4));

            Point pRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY()));
            Point pRight = transformPointFromGK4ToOSMCRS(pRightGK4, osmCRS);

            Point pDownGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() - distance));
            Point pDown = transformPointFromGK4ToOSMCRS(pDownGK4, osmCRS);

            Point pLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY()));
            Point pLeft = transformPointFromGK4ToOSMCRS(pLeftGK4, osmCRS);

            Point pUpGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() + distance));
            Point pUp = transformPointFromGK4ToOSMCRS(pUpGK4, osmCRS);

            Point pUpRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() + distance));
            Point pUpRight = transformPointFromGK4ToOSMCRS(pUpRightGK4, osmCRS);

            Point pDownRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() - distance));
            Point pDownRight = transformPointFromGK4ToOSMCRS(pDownRightGK4, osmCRS);

            Point pDownLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() - distance));
            Point pDownLeft = transformPointFromGK4ToOSMCRS(pDownLeftGK4, osmCRS);

            Point pUpLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() + distance));
            Point pUpLeft = transformPointFromGK4ToOSMCRS(pUpLeftGK4, osmCRS);

            for (SimpleFeature feature : landUseFeaturesBB.values()) {

                if (((Geometry) feature.getDefaultGeometry()).contains(pRight)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pDown)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pLeft)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pUp)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pUpRight)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pDownRight)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pDownLeft)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                } else if (((Geometry) feature.getDefaultGeometry()).contains(pUpLeft)) {
                    osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
                    return osmLandUseFeatureBBId;
                }
            }

            distance += 10.0;
        }

        log.warn("No area type found. Returning null...");
        return null;
    }

    @Deprecated
    private Point transformPointFromGK4ToOSMCRS(Point pointGK4, String osmCRS) {
        CoordinateTransformation ctGK4toOSMCRS = TransformationFactory.getCoordinateTransformation("EPSG:31468", osmCRS);

        Coord coordGK4 = MGC.coordinate2Coord(pointGK4.getCoordinate());
        Point pointOSMCRS = new GeometryFactory().createPoint(MGC.coord2Coordinate(ctGK4toOSMCRS.transform(coordGK4)));
        return pointOSMCRS;
    }

}