package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.router;

import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNEndpointID;

public interface Daemon2NECTARRoutingTable {
    int getMeetingFrequency(DTNEndpointID nodeEID);
    void incrementMeetingFrequency(DTNEndpointID nodeEID);
}