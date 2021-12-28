package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunBaseCaseHamburgScenario;
import org.opengis.feature.simple.SimpleFeature;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zmeng
 */
//TODO: change to static methods
public class GeneratePersonHomeLocation {

    private static final Logger log =Logger.getLogger(GeneratePersonHomeLocation.class);
    private Map<Person, Coord> person2homeLocation = new HashMap<>();
    private Map<Coord, String> coord2Area = new HashMap<>();

    private Scenario scenario;

    private static final String AREA_SHP_FILE = "../../svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_merge/hamburg.shp";

    public static void main(String[] args) throws IOException {

        String inputPlan = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v3/hamburg-v3.0-25pct.plans-not-calibrated.xml.gz";
        String outputResult = "../../svn/shared-svn/projects/matsim-hamburg/hamburg-v3/hamburg-v3.0-person2HomeLocation.tsv";
        GeneratePersonHomeLocation generatePersonHomeLocation = new GeneratePersonHomeLocation(inputPlan);
        generatePersonHomeLocation.generate();
        generatePersonHomeLocation.write(outputResult,"\t");

    }

    public GeneratePersonHomeLocation(String plansFile) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputCRS(RunBaseCaseHamburgScenario.COORDINATE_SYSTEM);
        config.plans().setInputFile(plansFile);
        this.scenario = ScenarioUtils.loadScenario(config);
    }

    private void generate() {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(AREA_SHP_FILE);


        for (Person person :
                scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            var activities = TripStructureUtils.getActivities(selectedPlan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
            for (Activity act :
                    activities) {
                if (act.getType().contains("home")){
                    this.person2homeLocation.put(person, act.getCoord());
                    this.coord2Area.computeIfAbsent(act.getCoord(), coord -> getArea(coord, features));
                    break;
                }
            }
        }
    }

    private void write(String outputFile, String splitSymbol) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(outputStreamWriter);
        bw.write("person" + splitSymbol + "home_x" + splitSymbol + "home_y" + splitSymbol + "area");

        this.person2homeLocation.forEach((person, coord) -> {
            try {
                bw.newLine();
                bw.write(person.getId().toString() + splitSymbol + coord.getX() + splitSymbol + coord.getY() + splitSymbol + coord2Area.get(coord));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        bw.close();
    }

    @Nullable
    private String getArea(Coord coord, Collection<SimpleFeature> features) {
        Point p = MGC.coord2Point(coord);
        for (SimpleFeature feature : features) {
            Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
            if (p.within(defaultGeometry)) {
                Long areaType = (Long) feature.getAttribute("AreaType");
                if (areaType == 0) {
                    return "Metropolregion";
                } else if (areaType == 1) {
                    return "HVV_Umland";
                } else if (areaType == 2) {
                    return "Hamburg_city";
                }
                throw new IllegalArgumentException();
            }
        }
        return "unknown";
    }
}
