package org.matsim.analysis.pt;

import com.google.inject.Inject;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author zmeng
 */
public class PtValidatorControlerListener implements ShutdownListener {

    @Inject
    PtValidatorConfigGroup ptValidatorConfigGroup;
    PtValidator ptValidator;

    public PtValidatorControlerListener(PtValidator ptValidator) {
        this.ptValidator = ptValidator;
    }

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        String files = ptValidatorConfigGroup.getSurveyCountsDirectory();

        System.out.println(ptValidator.getPtUsageMap());
    }
}
