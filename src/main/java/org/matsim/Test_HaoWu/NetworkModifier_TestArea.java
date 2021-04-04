package org.matsim.Test_HaoWu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

public class NetworkModifier_TestArea {

    //bigger BoundingBox:
    private static final double xMin = 556810.114452;
    private static final double yMin = 5928984.932974;
    private static final double xMax = 623954.716144;
    private static final double yMax = 5992509.470133;

    public static void main(String[] args) {
        String networkInputFile = "scenarios/input/hamburg-v1.1-network-with-pt.xml.gz";
        String networkOutputFile = "test/input/hamburg-v1.1-network-with-pt_Test_Hao.xml.gz";

        String areaShapeFile = "/Users/haowu/Workspace/QGIS/MATSim_HA2/NoCarZone_withRoundabout_fixed/NoCarZone_withRoundabout_fixed.shp";

        /*Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);

        Map<String, Geometry> zoneGeometries = new HashMap<>();
        for (SimpleFeature feature : features) {
            zoneGeometries.put((String)feature.getAttribute("Name"),(Geometry)feature.getDefaultGeometry());
        }

        Geometry areaGeometry = zoneGeometries.get(("NoCarZone"));*/
        Geometry areaGeometry = new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        //for(Link link : scenario.getNetwork().getLinks().values()){
        for (Map.Entry<Id<Link>, ? extends Link> entry : scenario.getNetwork().getLinks().entrySet()) {
            Link link = entry.getValue();
            if(link.getAllowedModes().contains(TransportMode.pt)){

            }else{
                Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
                if(areaGeometry.contains(linkCenterAsPoint)) {

                }else{
                    scenario.getNetwork().removeLink(entry.getKey());
                }
            }
        }

        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }
    
}
