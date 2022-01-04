package org.matsim.analysis.here;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTrip;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTripsExtractor;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsRouteValidator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author zmeng
 */
public class HereAPITravelTimeValidation {

    Config config;
    HereAPITravelTimeValidationConfigGroup hereAPITravelTimeValidationConfigGroup;
    CarTripsExtractor carTripsExtractor;

    public HereAPITravelTimeValidation(CarTripsExtractor carTripsExtractor, Config config) {
        this.carTripsExtractor = carTripsExtractor;
        this.config = config;
        this.hereAPITravelTimeValidationConfigGroup = ConfigUtils.addOrGetModule(config, HereAPITravelTimeValidationConfigGroup.class);
    }

    public void run() throws ParseException, MalformedURLException {

        CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(config.global().getCoordinateSystem(), TransformationFactory.WGS84);

        HereMapsRouteValidator travelTimeValidator = new HereMapsRouteValidator(config.controler().getOutputDirectory() + "/"
                + "here_validation_"
                + config.controler().getRunId() + "_"
                + hereAPITravelTimeValidationConfigGroup.getDate() + "/"
                + hereAPITravelTimeValidationConfigGroup.getTimeWindow()+ "/",
                hereAPITravelTimeValidationConfigGroup.getHereMapsAPIKey(),
                hereAPITravelTimeValidationConfigGroup.getDate(),
                transformation,
                true);

        travelTimeValidator.setWriteDetailedFiles(false);

        List<CarTrip> carTrips = carTripsExtractor.getTrips();
        String[] timeWindowString = hereAPITravelTimeValidationConfigGroup.getTimeWindow().split("-");

        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        double reference = (dateFormat.parse("00:00:00")).getTime();
        double time1 = (dateFormat.parse(timeWindowString[0]).getTime() - reference) / 1000L;
        double time2 = (dateFormat.parse(timeWindowString[1]).getTime() - reference) / 1000L;

        int numberOfTripsToValidate = (int) Double.POSITIVE_INFINITY;
        if(!hereAPITravelTimeValidationConfigGroup.getNumOfTrips().equals("all")){
            numberOfTripsToValidate = Integer.parseInt(hereAPITravelTimeValidationConfigGroup.getNumOfTrips());
        }


        int i = 0;
        Collections.shuffle(carTrips);
        Iterator var5 = carTrips.iterator();

        while(var5.hasNext()) {
            CarTrip trip = (CarTrip)var5.next();
            if (trip.getDepartureTime() >= time1 && trip.getDepartureTime() <= time2 && tripInResearchArea(trip)) {
                Tuple<Double, Double> timeDistance = travelTimeValidator.getTravelTime(trip);
                double validatedTravelTime = timeDistance.getFirst();
                trip.setValidatedTravelTime(validatedTravelTime);
                trip.setValidatedTravelDistance(timeDistance.getSecond());
                ++i;
            }

            if (i >= numberOfTripsToValidate) {
                break;
            }
        }

        if(hereAPITravelTimeValidationConfigGroup.getTmeBin() > 0){
            for (double j = time1; j < time2 ; j=j + hereAPITravelTimeValidationConfigGroup.getTmeBin()) {
                this.writeTravelTimeValidation(carTrips, j, j+ hereAPITravelTimeValidationConfigGroup.getTmeBin());
            }
        }
        this.writeTravelTimeValidation(carTrips,time1,time2);
    }

    private boolean tripInResearchArea(CarTrip trip) throws MalformedURLException {
        if(hereAPITravelTimeValidationConfigGroup.getResearchAreaShapeFile() == null)
            return true;
        else {
            Coord coord1 = trip.getDepartureLocation();
            Coord coord2 = trip.getArrivalLocation();

            boolean coord1InArea = false;
            boolean coord2InArea = false;

            Collection<SimpleFeature> simpleFeatures;
            if(hereAPITravelTimeValidationConfigGroup.getResearchAreaShapeFile().contains("http")){
                simpleFeatures = ShapeFileReader.getAllFeatures(new URL(hereAPITravelTimeValidationConfigGroup.getResearchAreaShapeFile()));
            } else {
                simpleFeatures = ShapeFileReader.getAllFeatures(hereAPITravelTimeValidationConfigGroup.getResearchAreaShapeFile());
            }

            for (SimpleFeature simpleFeature : simpleFeatures) {
                if(coord1InArea && coord2InArea)
                    break;
                Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();

                if(!coord1InArea){
                    Point point1 = MGC.coord2Point(coord1);
                    coord1InArea = point1.within(geometry);
                }

                if(!coord2InArea){
                    Point point2 = MGC.coord2Point(coord2);
                    coord2InArea = point2.within(geometry);
                }
            }
            return coord1InArea && coord2InArea;
        }
    }

    private String seconds2hhmmss(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    private void writeTravelTimeValidation(List<CarTrip> trips, double time1, double time2) {

        String folder = config.controler().getOutputDirectory() + "/"
                + "here_validation_"
                + config.controler().getRunId() + "_"
                + hereAPITravelTimeValidationConfigGroup.getDate() + "/"
                + seconds2hhmmss((long) time1) + "-" + seconds2hhmmss((long) time2) + "/";

        File outDir = new File(folder);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        BufferedWriter bw = IOUtils.getBufferedWriter(folder + "/validated_trips.csv");
        XYSeriesCollection times = new XYSeriesCollection();
        XYSeriesCollection distances = new XYSeriesCollection();
        XYSeries distancess = new XYSeries("distances", true, true);
        XYSeries timess = new XYSeries("times", true, true);
        times.addSeries(timess);
        distances.addSeries(distancess);

        try {
            bw.append("agent;departureTime;fromX;fromY;toX;toY;traveltimeActual;traveltimeValidated;traveledDistance;validatedDistance");
            Iterator var8 = trips.iterator();

            while(var8.hasNext()) {
                CarTrip trip = (CarTrip)var8.next();
                if (trip.getValidatedTravelTime() != null && trip.getDepartureTime() >= time1 && trip.getDepartureTime() < time2) {
                    bw.newLine();
                    bw.append(trip.toString());
                    timess.add(trip.getActualTravelTime(), trip.getValidatedTravelTime());
                    distancess.add(trip.getTravelledDistance(), trip.getValidatedTravelDistance());
                }
            }

            bw.flush();
            bw.close();
            JFreeChart chart2 = ChartFactory.createScatterPlot("Travel Times", "Simulated travel time [s]", "Validated travel time [s]", times);
            JFreeChart chart = ChartFactory.createScatterPlot("Travel Distances", "Simulated travel distance [m]", "Validated travel distance [m]", distances);
            NumberAxis yAxis = (NumberAxis)((XYPlot)chart2.getPlot()).getRangeAxis();
            NumberAxis xAxis = (NumberAxis)((XYPlot)chart2.getPlot()).getDomainAxis();
            NumberAxis yAxisd = (NumberAxis)((XYPlot)chart.getPlot()).getRangeAxis();
            NumberAxis xAxisd = (NumberAxis)((XYPlot)chart.getPlot()).getDomainAxis();
            yAxisd.setUpperBound(xAxisd.getUpperBound());
            yAxis.setUpperBound(xAxis.getUpperBound());
            yAxis.setTickUnit(new NumberTickUnit(500.0D));
            xAxis.setTickUnit(new NumberTickUnit(500.0D));
            XYAnnotation diagonal = new XYLineAnnotation(xAxis.getRange().getLowerBound(), yAxis.getRange().getLowerBound(), xAxis.getRange().getUpperBound(), yAxis.getRange().getUpperBound());
            ((XYPlot)chart2.getPlot()).addAnnotation(diagonal);
            XYAnnotation diagonald = new XYLineAnnotation(xAxisd.getRange().getLowerBound(), yAxisd.getRange().getLowerBound(), xAxisd.getRange().getUpperBound(), yAxisd.getRange().getUpperBound());
            ((XYPlot)chart.getPlot()).addAnnotation(diagonald);
            ChartUtils.writeChartAsPNG(new FileOutputStream(folder + "/validated_traveltimes.png"), chart2, 1500, 1500);
            ChartUtils.writeChartAsPNG(new FileOutputStream(folder + "/validated_traveldistances.png"), chart, 1500, 1500);
        } catch (IOException var16) {
            var16.printStackTrace();
        }

    }
}
