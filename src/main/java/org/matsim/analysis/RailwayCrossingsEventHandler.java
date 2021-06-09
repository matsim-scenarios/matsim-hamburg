package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

import java.util.HashMap;

public class RailwayCrossingsEventHandler implements LinkEnterEventHandler {


    private final HashMap<Id, Double> carLinkMap;
    private final HashMap<Id, Double> carLinkTimeMap;
    private final HashMap<Id, Double> ptLinkMap;
    private final HashMap<Id, Double> ptLinkTimeMap;

    public RailwayCrossingsEventHandler(HashMap<Id, Double> carLinkMap, HashMap<Id, Double> carLinkTimeMap, HashMap<Id, Double> ptLinkMap, HashMap<Id, Double> ptLinkTimeMap, HashMap<Id, Id> conection) {
        this.carLinkMap = carLinkMap;
        this.carLinkTimeMap = carLinkTimeMap;
        this.ptLinkMap = ptLinkMap;
        this.ptLinkTimeMap = ptLinkTimeMap;
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {

        Id linkId = linkEnterEvent.getLinkId();

        if (RailwayCrossings.carLinkMap.containsKey(linkId)) {
            RailwayCrossings.carLinkTimeMap.put(linkId, linkEnterEvent.getTime());
        }

        if (RailwayCrossings.ptLinkMap.containsKey(linkId)) {
            RailwayCrossings.ptLinkTimeMap.put(linkId, linkEnterEvent.getTime());
        }
    }



}
