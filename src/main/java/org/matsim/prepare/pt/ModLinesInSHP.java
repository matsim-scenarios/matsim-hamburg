package org.matsim.prepare.pt;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModLinesInSHP {

    static List<TransitRoute> trToDelete = new ArrayList<>();
    static List<TransitLine> tlToDelete = new ArrayList<>();
    private static final Logger log = Logger.getLogger(ModLinesInSHP.class);

    public static void main(String[] args) {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {
                    "scenarios/input/hamburg-v2.0-10pct.config.xml", // [0] "Config file path",
                    "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/network/hamburg-v2.0-reallab2030plus-network-with-pt.xml.gz", // [1] "Network with pt path",
                    "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/pt/hamburg-v2.0-reallab2030plus-transitSchedule.xml.gz", // [2] "TransitSchedule input Path",
                    "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v2/hamburg-v2.0/reallab2030plus/input/pt/hamburg-v2.0-reallab2030plus-transitSchedule.xml.gz", // [3] "TransitSchedule output Path",
                    "https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_hvv_incl_gtfs2019stops/hamburg_hvv_new.shp", // [4] "shp file path"
//                    "scenarios/input/hamburg_hvv_new.shp" // [4] "shp file path"
            };
        }

        ModLinesInSHP routeModifier = new ModLinesInSHP();
        TransitSchedule tsInput = routeModifier.getTransitSchedule(args[0], args[1], args[2]);
        routeModifier.modify(tsInput, args[3], args[4]);
    }

    private TransitSchedule getTransitSchedule(String configPath, String networkPath, String tsInputPath){
        Config config = ConfigUtils.loadConfig(configPath);
        config.network().setInputFile(networkPath);
        config.transit().setTransitScheduleFile(tsInputPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getTransitSchedule();
    }


    private void modify(TransitSchedule tsInput, String tsOutputPath, String shpPath){
        TransitSchedule tsOutput = tsInput;

        List<PreparedGeometry> polygons = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shpPath));
        int inShpCtr, tlDelCtr = 0, trDelCtr = 0;
//    Iterating through all stops to detect which ones to delete
        for (TransitLine tl : tsOutput.getTransitLines().values()) {
            for (TransitRoute tr : tl.getRoutes().values()) {
                inShpCtr = 0;
                for (TransitRouteStop trStop : tr.getStops()) {
                    if (ShpGeometryUtils.isCoordInPreparedGeometries(trStop.getStopFacility().getCoord(), polygons)) {
                        inShpCtr++;
                        break;
                    }
                }
                if (inShpCtr == 0) {
                    trToDelete.add(tr);
                }
            }
            for(TransitRoute trDel : trToDelete){
                if(tl.getRoutes().keySet().contains(trDel.getId())) {
                    System.out.println("Removing Route " + trDel.getId().toString() + " ...");
                    tl.removeRoute(trDel);
                    trDelCtr++;
                }
            }

            if (tl.getRoutes().size() == 0) {
                tlToDelete.add(tl);
            }
        }

        for(TransitLine tlDel : tlToDelete){
            System.out.println("Removing Line " + tlDel.getId().toString() + " ...");
            tsOutput.removeTransitLine(tlDel);
            tlDelCtr++;
        }

        System.out.println("Write new TransitSchedule to " + tsOutputPath + " ...");
        new TransitScheduleWriter(tsOutput).writeFile(tsOutputPath);
        System.out.println("Done. Summary:");
        System.out.println("In total " + tlDelCtr + " TransitLines have been deleted. Check details with getTlToDelete()");
        System.out.println("In total " + trDelCtr + " TransitRoutes have been deleted. Check details with getTrToDelete()\n");
    }

    public List<TransitRoute> getTrToDelete () {
        return trToDelete;
    }

    public List<TransitLine> getTlToDelete () {
        return tlToDelete;
    }

}
