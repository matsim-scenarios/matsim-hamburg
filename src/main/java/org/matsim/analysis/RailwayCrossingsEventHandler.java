package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.*;

public class RailwayCrossingsEventHandler implements LinkEnterEventHandler {


    private final HashMap<Id, Double> carLinkMap;
    private final HashMap<Id, List<Double>> carLinkTimeMap;
    private final HashMap<Id, Double> ptLinkMap;
    private final HashMap<Id, List<Double>> ptLinkTimeMap;

    public RailwayCrossingsEventHandler(HashMap<Id, Double> carLinkMap, HashMap<Id, List<Double>> carLinkTimeMap, HashMap<Id, Double> ptLinkMap, HashMap<Id, List<Double>> ptLinkTimeMap) {
        this.carLinkMap = carLinkMap;
        this.carLinkTimeMap = carLinkTimeMap;
        this.ptLinkMap = ptLinkMap;
        this.ptLinkTimeMap = ptLinkTimeMap;
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {

        Id linkId = linkEnterEvent.getLinkId();

        if (carLinkMap.containsKey(linkId)) {
            double time = linkEnterEvent.getTime();
            if (!carLinkTimeMap.containsKey(linkId)) {
                List<Double> timeList = new ArrayList<>();
                carLinkTimeMap.put(linkId, timeList);
            }
            if (carLinkTimeMap.containsKey(linkId)) {
                carLinkTimeMap.get(linkId).add(time);
            }
        }

        if (ptLinkMap.containsKey(linkId)) {
            double time = linkEnterEvent.getTime();
            if (!ptLinkTimeMap.containsKey(linkId)) {
                List<Double> timeList = new ArrayList<>();
                ptLinkTimeMap.put(linkId, timeList);
            }
            if (ptLinkTimeMap.containsKey(linkId)) {
                ptLinkTimeMap.get(linkId).add(time);
            }
        }
    }



}
