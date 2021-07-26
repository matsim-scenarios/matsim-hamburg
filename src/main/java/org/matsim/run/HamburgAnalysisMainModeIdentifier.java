package org.matsim.run;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.prepare.freight.AdjustScenarioForFreight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author zmeng
 */
//TODO remove this class
public class HamburgAnalysisMainModeIdentifier implements AnalysisMainModeIdentifier {
    private final List<String> modeHierarchy = new ArrayList();
    private final List<String> drtModes = Arrays.asList("drt", "drt1", "drt2", "drt_teleportation", RunDRTFeederScenario.DRT_FEEDER_MODE);
    private final List<String> freightModes;

    @Inject
    public HamburgAnalysisMainModeIdentifier() {
        this.freightModes = AdjustScenarioForFreight.getFreightModes();
        this.modeHierarchy.add("transit_walk");
        this.modeHierarchy.add("walk");
        this.modeHierarchy.add("bike");
        this.modeHierarchy.add("bicycle");
        this.modeHierarchy.add("ride");
        this.modeHierarchy.add("car");
        this.modeHierarchy.add("scar");
        this.modeHierarchy.add("sbike");

        Iterator var1 = this.drtModes.iterator();

        while(var1.hasNext()) {
            String drtMode = (String)var1.next();
            this.modeHierarchy.add(drtMode);
        }

        this.modeHierarchy.add("pt");
        this.modeHierarchy.addAll(freightModes);
    }

    public String identifyMainMode(List<? extends PlanElement> planElements) {
        int mainModeIndex = -1;
        Iterator var3 = planElements.iterator();

        while(true) {
            String mode;
            do {
                do {
                    do {
                        PlanElement pe;
                        do {
                            if (!var3.hasNext()) {
                                if (mainModeIndex == -1) {
                                    throw new RuntimeException("no main mode found for trip " + planElements);
                                }

                                return this.modeHierarchy.get(mainModeIndex);
                            }

                            pe = (PlanElement)var3.next();
                        } while(!(pe instanceof Leg));

                        Leg leg = (Leg)pe;
                        mode = leg.getMode();
                    } while(mode.equals("non_network_walk"));
                } while(mode.equals("access_walk"));
            } while(mode.equals("egress_walk"));

            if (mode.equals("transit_walk")) {
                mode = "transit_walk";
            } else {
                Iterator var9 = this.drtModes.iterator();

                while(var9.hasNext()) {
                    String drtMode = (String)var9.next();
                    if (mode.equals(drtMode + "_fallback")) {
                        mode = drtMode;
                    }
                }
            }

            int index = this.modeHierarchy.indexOf(mode);
            if (index < 0) {
                throw new RuntimeException("unknown mode=" + mode);
            }

            if (index > mainModeIndex) {
                mainModeIndex = index;
            }
        }
    }
}
