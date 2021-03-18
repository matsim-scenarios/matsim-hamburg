package org.matsim.analysis;

import com.google.inject.Inject;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author zmeng
 */
public class PlanBasedTripsWriterControlerListener implements ShutdownListener {
    @Inject
    PlanBasedTripsFileWriter planBasedTripsFileWriter;

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        planBasedTripsFileWriter.write();
    }
}
