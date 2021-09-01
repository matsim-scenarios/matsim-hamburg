package org.matsim.prepare;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.ArrayList;
import java.util.List;

public class ModLinesInSHP {

    TransitSchedule tsInput = null;
    TransitSchedule tsOutput = null;
    String shpPath = "scenarios/berlin-v5.4-1pct/input/SHP.shp";
    String tsOutputPath = "./transitScheduleName_cut.xml.gz";
    List<TransitRoute> trToDelete = new ArrayList<>();
    List<TransitLine> tlToDelete = new ArrayList<>();

    ModLinesInSHP(){
        System.out.println("No inputs found. Using presets ...");
        modify();
    }

    ModLinesInSHP(TransitSchedule transitScheduleInput, String shpPathInput, String transitScheduleOutputPath){
        tsInput = transitScheduleInput;
        shpPath = shpPathInput;
        tsOutputPath = transitScheduleOutputPath;
        modify();
    }

    void modify(){
        List<PreparedGeometry> polygons = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shpPath));
        tsOutput = tsInput;
        int inShpCtr, tlDelCtr = 0, trDelCtr = 0;
//    Iterating through all stops to detect which ones to delete
        for(TransitLine tl:tsOutput.getTransitLines().values()){
            for(TransitRoute tr:tl.getRoutes().values()){
                inShpCtr = 0;
                for(TransitRouteStop trStop:tr.getStops()){
                    if(ShpGeometryUtils.isCoordInPreparedGeometries(trStop.getStopFacility().getCoord(), polygons)){
                        inShpCtr++;
                        break;
                    }
                }
                if(inShpCtr == 0){
                    trToDelete.add(tr);
                    System.out.println("Removing Route "+tr.getId().toString()+" ...");
                    tl.removeRoute(tr);
                    trDelCtr++;
                }
            }
            if(tl.getRoutes().size() == 0){
                tlToDelete.add(tl);
                System.out.println("Removing Line "+tl.getId().toString()+" ...");
                tsOutput.removeTransitLine(tl);
                tlDelCtr++;
            }
        }
        System.out.println("Write new TransitSchedule to "+tsOutputPath+" ...");
        new TransitScheduleWriter(tsOutput).writeFile(tsOutputPath);
        System.out.println("Done. Summary:");
        System.out.println("In total "+tlDelCtr+" TransitLines have been deleted. Check details with getTlToDelete()\n");
        System.out.println("In total "+trDelCtr+" TransitRoutes have been deleted. Check details with getTrToDelete()\n");
    }

    public List<TransitRoute> getTrToDelete() {
        return trToDelete;
    }

    public List<TransitLine> getTlToDelete() {
        return tlToDelete;
    }
}
