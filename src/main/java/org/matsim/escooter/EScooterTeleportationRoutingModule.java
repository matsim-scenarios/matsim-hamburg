package org.matsim.escooter;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.run.HamburgShpUtils;

import java.util.List;

/**
 * @author zmeng
 */
public class EScooterTeleportationRoutingModule extends TeleportationRoutingModule {
    @Inject
    EScooterConfigGroup eScooterConfigGroup;
    @Inject
    public EScooterTeleportationRoutingModule(String mode, Scenario scenario, double networkTravelSpeed, double beelineDistanceFactor) {
        super(mode, scenario, networkTravelSpeed, beelineDistanceFactor);
    }

    @Override
    public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {

        if(inServiceArea(fromFacility, toFacility))
            return super.calcRoute(fromFacility, toFacility, departureTime, person);
        else
            return null;
    }

    private boolean inServiceArea(Facility fromFacility, Facility toFacility) {
        String serviceArea = eScooterConfigGroup.getEScooterServiceArea();
        if (serviceArea == null){
            return true;
        } else {
            HamburgShpUtils hamburgShpUtils = new HamburgShpUtils(eScooterConfigGroup.getEScooterServiceArea());
            return hamburgShpUtils.isCoordInServiceArea(fromFacility.getCoord()) && hamburgShpUtils.isCoordInServiceArea(toFacility.getCoord());
        }

    }
}
