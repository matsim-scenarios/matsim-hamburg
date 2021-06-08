package org.matsim.run;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.sharing.logic.SharingEngine;
import org.matsim.contrib.sharing.logic.SharingLogic;
import org.matsim.contrib.sharing.service.SharingService;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * @author zmeng
 */
public class Dummy extends AbstractDvrpModeQSimModule {

    protected Dummy(String mode) {
        super(mode);
    }

    @Override
    protected void configureQSim() {
        this.addModalComponent(SharingEngine.class, this.modalProvider((getter) -> {
            EventsManager eventsManager = (EventsManager)getter.get(EventsManager.class);
            SharingLogic logic = (SharingLogic)getter.getModal(SharingLogic.class);
            SharingService service = (SharingService)getter.getModal(SharingService.class);
            return new SharingEngine(service, logic, eventsManager);
        }));
    }
}
