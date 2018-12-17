package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app;

import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PrimaryBlock;

public interface DTNAPI {
    void send(
        byte[] message, String recipient, PrimaryBlock.PriorityClass priorityClass,
        PrimaryBlock.LifeTime lifetime
    );
}