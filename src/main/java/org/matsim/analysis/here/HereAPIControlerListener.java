package org.matsim.analysis.here;

import com.google.inject.Inject;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.text.ParseException;

/**
 * @author zmeng
 */
public class HereAPIControlerListener implements ShutdownListener {

    @Inject
    HereAPITravelTimeValidation hereAPITravelTimeValidation;

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        try {
            hereAPITravelTimeValidation.run();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
