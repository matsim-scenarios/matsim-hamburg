package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.HashMap;
import java.util.List;

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
            carLinkTimeMap.get(linkId).add(linkEnterEvent.getTime());
        }

        if (ptLinkMap.containsKey(linkId)) {
            ptLinkTimeMap.get(linkId).add(linkEnterEvent.getTime());
        }
    }



}
