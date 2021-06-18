package org.matsim.sharingFare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.events.SharingPickupEvent;
import org.matsim.contrib.sharing.service.events.SharingPickupEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author zmeng
 */
public class TeleportedSharingFareHandler implements ActivityEndEventHandler, SharingPickupEventHandler {

    private final String PICK_UP_EVENT = "sharing pickup interaction";
    private final String DROP_OFF_EVENT = "sharing dropoff interaction";

    @Inject
    Network network;

    @Inject
    EventsManager eventsManager;

    @Inject
    SharingConfigGroup sharingConfigGroup;

    @Inject
    SharingFaresConfigGroup sharingFaresConfigGroup;

    @Inject
    PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

    private SharingServiceFaresConfigGroup sharingServiceFaresConfigGroup;
    private SharingServiceConfigGroup sharingServiceConfigGroup;
    private String serviceId;
    private String mode;
    private Map<Id<Person>, TeleportedSharingTrips> sharingTrips = new HashMap<>();
    private Set<Id<Person>> sharingCustomers = new HashSet<>();

    public TeleportedSharingFareHandler(String serviceId) {
        this.serviceId = serviceId;
    }


    @Override
    public void reset(int iteration) {
        sharingTrips.clear();

        for (SharingServiceConfigGroup sharingServiceConfigGroup : sharingConfigGroup.getServices()) {
            if(sharingServiceConfigGroup.getId().equals(serviceId)){
                this.sharingServiceConfigGroup = sharingServiceConfigGroup;
                break;
            }
        }
        this.mode = this.sharingServiceConfigGroup.getMode();
        this.sharingServiceFaresConfigGroup = sharingFaresConfigGroup.getServiceFareParams(serviceId);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals(PICK_UP_EVENT) && this.sharingCustomers.contains(event.getPersonId())){
            if(this.sharingTrips.containsKey(event.getPersonId()) && (!this.sharingTrips.get(event.getPersonId()).isCharged()))
                throw new RuntimeException("person " + event.getPersonId() + " is already registered for a sharing trip");

            TeleportedSharingTrips sharingTrip = new TeleportedSharingTrips(event.getPersonId(), event);
            sharingTrips.put(event.getPersonId(),sharingTrip);
        } else if(event.getActType().equals(DROP_OFF_EVENT) && this.sharingCustomers.contains(event.getPersonId())){
            if(!this.sharingTrips.containsKey(event.getPersonId()))
                throw new RuntimeException("person " + event.getPersonId() + " has no pick up event");
            else {
                this.sharingTrips.get(event.getPersonId()).setDropoffEvent(event);
                this.sharingCustomers.remove(event.getPersonId());
            }
        }

    }

    @Override
    public void handleEvent(SharingPickupEvent event) {
        if(event.getServiceId().toString().equals(this.serviceId)){
            this.sharingCustomers.add(event.getPersonId());
        }
    }

    private class TeleportedSharingTrips {
        Id<Person> personId;
        ActivityEndEvent pickupEvent;
        ActivityEndEvent dropoffEvent;

        double tripDis;
        double travelTimeInSec;
        double fare;
        String tripDescription;
        boolean isCharged = false;

        private TeleportedSharingTrips(Id<Person> personId, ActivityEndEvent pickupEvent) {
            this.personId = personId;
            this.pickupEvent = pickupEvent;
        }

        private void setDropoffEvent(ActivityEndEvent dropoffEvent) {
            if (isCharged)
                throw new RuntimeException("something wrong because this trip is already charged");
            this.dropoffEvent = dropoffEvent;
            this.travelTimeInSec = dropoffEvent.getTime() - this.pickupEvent.getTime();
            setTripFare();
            setTripDis();
            tripDescription = "travel with sharing service " + serviceId + " for " + this.tripDis + " meters and " + this.travelTimeInSec + " seconds";
            eventsManager.processEvent(new PersonMoneyEvent(dropoffEvent.getTime(),personId,-fare,tripDescription,serviceId));
            isCharged = true;
        }

        private void setTripDis() {
            Coord fromActCoord = network.getLinks().get(pickupEvent.getLinkId()).getCoord();
            Coord toActCoord = network.getLinks().get(dropoffEvent.getLinkId()).getCoord();
            double dist = CoordUtils.calcEuclideanDistance(fromActCoord, toActCoord);

            double factor = plansCalcRouteConfigGroup.getBeelineDistanceFactors().get(mode);
            this.tripDis = dist * factor;
        }


        private void setTripFare(){
            this.fare = sharingServiceFaresConfigGroup.getBasefare() +
                    sharingServiceFaresConfigGroup.getDistanceFare_m() * this.tripDis +
                    sharingServiceFaresConfigGroup.getTimeFare_m() * Math.ceil(this.travelTimeInSec / 60.);
        }

        private boolean isCharged() {
            return isCharged;
        }

    }
}
