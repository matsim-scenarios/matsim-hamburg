package org.matsim.prepare.population;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@CommandLine.Command(
        name = "population",
        description = "Add and standardize the person attribute to the Hamburg raw population file"
)
public class PreparePopulationAttributes implements MATSimAppCommand {
    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
    private Path input;

    @CommandLine.Option(names = "--attributes", description = "Path to attribute file")
    private Path attributesFile;

    @CommandLine.Option(names = "--output", description = "Path to output population", required = true)
    private Path outputPath;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();

    public static void main(String[] args) {
        new PreparePopulationAttributes().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Random rnd = new Random(1234);
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(crs.getInputCRS());

        Geometry studyArea = null;
        if (shp.getShapeFile() != null) {
            studyArea = shp.getGeometry();
        }

        if (attributesFile != null) {
            config.plans().setInputPersonAttributeFile(attributesFile.toString());  //TODO how to do this properly???
            config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
        }
        config.plans().setInputFile(input.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        if (!Files.exists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        // Standardize person attribute (age, sex, car availability, income...)
        for (Person person : population.getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            person.getPlans().clear();
            person.addPlan(selectedPlan);

            if (person.getAttributes().getAttribute("gender") != null) {
                String sex = person.getAttributes().getAttribute("gender").toString();
                PersonUtils.setSex(person, sex);
                person.getAttributes().removeAttribute("gender");
            }

            if (person.getAttributes().getAttribute("sim_carAvailability") != null) {
                String carAvailability = person.getAttributes().getAttribute("sim_carAvailability").toString();
                PersonUtils.setCarAvail(person, carAvailability);
                person.getAttributes().removeAttribute("sim_carAvailability");
            }

            if (person.getAttributes().getAttribute("householdincome") != null &&
                    person.getAttributes().getAttribute("householdsize") != null) {
                int incomeGroup = Integer.parseInt(person.getAttributes().getAttribute("householdincome").toString());
                double householdSize = Double.parseDouble(person.getAttributes().getAttribute("householdsize").toString());

                double income = 0;
                switch (incomeGroup) {
                    case 1:
                        income = 500 / householdSize;
                        break;
                    case 2:
                        income = (rnd.nextInt(400) + 500) / householdSize;
                        break;
                    case 3:
                        income = (rnd.nextInt(600) + 900) / householdSize;
                        break;
                    case 4:
                        income = (rnd.nextInt(500) + 1500) / householdSize;
                        break;
                    case 5:
                        income = (rnd.nextInt(1000) + 2000) / householdSize;
                        break;
                    case 6:
                        income = (rnd.nextInt(1000) + 3000) / householdSize;
                        break;
                    case 7:
                        income = (rnd.nextInt(1000) + 4000) / householdSize;
                        break;
                    case 8:
                        income = (rnd.nextInt(1000) + 5000) / householdSize;
                        break;
                    case 9:
                        income = (rnd.nextInt(1000) + 6000) / householdSize;
                        break;
                    case 10:
                        income = (Math.abs(rnd.nextGaussian()) * 1000 + 7000) / householdSize;
                        break;
                    default:
                        income = 2364; // Average monthly household income per Capita (2021). See comments below for details
                        break;
                    // Average Gross household income: 4734 Euro
                    // Average household size: 83.1M persons /41.5M households = 2.0 persons / household
                    // Average household income per capita: 4734/2.0 = 2364 Euro
                    // Source (Access date: 21 Sep. 2021):
                    // https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
                    // https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
                    // https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html;jsessionid=E0D7A060D654B31C3045AAB1E884CA75.live711
                }
                PersonUtils.setIncome(person, income);
            }


            // Add an attribute for person home location (i.e. inside or outside the study area
            if (studyArea != null) {
                if (checkIfPersonLivesInArea(person, studyArea)) {
                    person.getAttributes().putAttribute("homeLocation", "inside"); // TODO there is a enum in the MATSim Kelheim scenario. That one will be moved to the matsim-lib. After that this one can be updated also
                } else {
                    person.getAttributes().putAttribute("homeLocation", "outside");
                }
            } else {
                person.getAttributes().putAttribute("homeLocation", "unknown");
            }
        }

        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write(outputPath.toString());
        return 0;
    }

    public static boolean checkIfPersonLivesInArea(Person person, Geometry analyzedArea) {
        for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
            if (planElement instanceof Activity) {
                String actType = ((Activity) planElement).getType();
                if (actType.startsWith("home")) {
                    Coord homeCoord = ((Activity) planElement).getCoord();
                    if (analyzedArea == null) {
                        throw new RuntimeException("The analyzed area is null! ");
                    } else return analyzedArea.contains(MGC.coord2Point(homeCoord));
                }
            }
        }
        return false; // Person with no home activity --> false
    }
}
