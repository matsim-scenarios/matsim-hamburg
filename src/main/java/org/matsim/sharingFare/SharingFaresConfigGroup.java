package org.matsim.sharingFare;

import org.apache.log4j.Logger;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zmeng
 */
public class SharingFaresConfigGroup extends ReflectiveConfigGroup {
    private static final Logger logger = Logger.getLogger(SharingFaresConfigGroup.class);
    public static final String GROUP_NAME = "sharingfares";

    public SharingFaresConfigGroup() {
        super(GROUP_NAME);
    }

    public void addServiceFaresParams(SharingServiceFaresConfigGroup sharingServiceFareParams){this.addParameterSet(sharingServiceFareParams);}

    public Collection<SharingServiceFaresConfigGroup> getAllServiceFares() {
        List<SharingServiceFaresConfigGroup> serviceFares = new LinkedList();
        Iterator var2 = this.getParameterSets("mode").iterator();



        while(var2.hasNext()) {
            ConfigGroup set = (ConfigGroup)var2.next();
            SharingServiceFaresConfigGroup sharingServiceFaresConfigGroup = new SharingServiceFaresConfigGroup();

            sharingServiceFaresConfigGroup.setId(set.getParams().get("id"));

            if(set.getParams().containsKey("distanceFare_m"))
                sharingServiceFaresConfigGroup.setDistanceFare_m(Double.parseDouble(set.getParams().get("distanceFare_m")));
            if(set.getParams().containsKey("basefare"))
                sharingServiceFaresConfigGroup.setBasefare(Double.parseDouble(set.getParams().get("basefare")));
            if(set.getParams().containsKey("timeFare_m"))
                sharingServiceFaresConfigGroup.setTimeFare_m(Double.parseDouble(set.getParams().get("timeFare_m")));

            serviceFares.add(sharingServiceFaresConfigGroup);
        }

        return Collections.unmodifiableList(serviceFares);
    }

    public SharingServiceFaresConfigGroup getServiceFareParams(String id){
        SharingServiceFaresConfigGroup sharingServiceFaresConfigGroup1 = null;
        for (SharingServiceFaresConfigGroup sharingServiceFaresConfigGroup : this.getAllServiceFares()) {
            if(sharingServiceFaresConfigGroup.getId().equals(id)){
                sharingServiceFaresConfigGroup1 = sharingServiceFaresConfigGroup;
                break;
            }
        }
        if(sharingServiceFaresConfigGroup1 != null)
            return sharingServiceFaresConfigGroup1;
        else
            throw new RuntimeException("fare-settings for sharing service with id " + id + " are missing");
    }

    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        (new BeanValidationConfigConsistencyChecker()).checkConsistency(config);
        Set<String> fareIds = new HashSet();
        Iterator var3 = this.getAllServiceFares().iterator();

        SharingConfigGroup sharingConfigGroup = ConfigUtils.addOrGetModule(config, SharingConfigGroup.class);
        var serviceIds = sharingConfigGroup.getServices().stream().map(SharingServiceConfigGroup::getId).collect(Collectors.toList());


        while(var3.hasNext()) {
            SharingServiceFaresConfigGroup sharingServiceFaresConfigGroup = (SharingServiceFaresConfigGroup) var3.next();
            if (fareIds.contains(sharingServiceFaresConfigGroup.getId())) {
                throw new IllegalStateException("Duplicate sharing service: " + sharingServiceFaresConfigGroup.getId());
            }

            fareIds.add(sharingServiceFaresConfigGroup.getId());
        }

        for (String id : serviceIds) {
            if (!fareIds.contains(id)) {
                SharingServiceFaresConfigGroup faresConfigGroup = new SharingServiceFaresConfigGroup();
                faresConfigGroup.setId(id);

                this.addParameterSet(faresConfigGroup);
                logger.warn("none existing fare settings for sharing service " + id + ", a default fare with all 0 is added");
            }
        }

    }


}
