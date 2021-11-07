/*
package org.matsim.prepare;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.RailwayCrossings;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestTimeDiffCalculation {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runTest() throws IOException {

        RailwayCrossings.carLinkTimeMap = new HashMap<>();
        List<Double> listOfCarTime = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        RailwayCrossings.carLinkTimeMap.put(Id.createLinkId("1"), listOfCarTime);

        RailwayCrossings.ptLinkMap = new HashMap<>();
        List<Double> listOfPtTime = Arrays.asList(2.0, 2.0, 13.0);
        RailwayCrossings.connection.put(Id.createLinkId("1"), Id.createLinkId("2"));

        RailwayCrossings.ptLinkTimeMap.put(Id.createLinkId("2"), listOfPtTime);
        HashMap<Tuple<Id<Link>, Id<Link>>, Integer> amountOfCriticalPassings = RailwayCrossings.calculateTimeDiff();
        RailwayCrossings.writeResults2CSV(amountOfCriticalPassings);


    }



    }
*/
