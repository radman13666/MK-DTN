package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn;

import android.annotation.SuppressLint;

import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app.DTNAPI;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app.DTNUI;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app.Daemon2AppAA;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.daemon.AppAA2Daemon;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.CanonicalBlock;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundle;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundleID;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNEndpointID;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PayloadADU;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PrimaryBlock;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

final class RadAppAA implements DTNAPI, Daemon2AppAA {
//    private static final String LOG_TAG
//        = DTNUtils.MAIN_LOG_TAG + "_" + RadAppAA.class.getSimpleName();
    
    
    private DTNUI ui;
    private AppAA2Daemon daemon;
    
    RadAppAA(DTNUI ui, AppAA2Daemon daemon) {
        this.ui = ui;
        this.daemon = daemon;
    }
    
    @Override
    public void send(
        byte[] message, String recipient, PrimaryBlock.PriorityClass priorityClass,
        PrimaryBlock.LifeTime lifeTime
    ) {
        DTNEndpointID receiver = DTNEndpointID.from(DTNEndpointID.DTN_SCHEME, recipient);
        DTNBundle bundleToSend
            = createUserBundle(message, receiver, priorityClass, lifeTime);
        
        daemon.transmit(bundleToSend);
    }
    
    @SuppressLint("UseSparseArrays")
    private DTNBundle createUserBundle(
        byte[] message, DTNEndpointID recipient, PrimaryBlock.PriorityClass priorityClass,
        PrimaryBlock.LifeTime lifetime
    ) {
        PrimaryBlock primaryBlock = makePrimaryBlock(recipient, priorityClass, lifetime);
    
        CanonicalBlock ageCBlock
            = DTNUtils.makeAgeCBlock(primaryBlock.bundleID.creationTimestamp);
        
        CanonicalBlock payloadCBlock = makePayloadCBlock(message);
    
        DTNBundle userBundle = new DTNBundle();
        userBundle.primaryBlock = primaryBlock;
        userBundle.canonicalBlocks = new HashMap<>();
        userBundle.canonicalBlocks.put(DTNBundle.CBlockNumber.PAYLOAD, payloadCBlock);
        userBundle.canonicalBlocks.put(DTNBundle.CBlockNumber.AGE, ageCBlock);
    
        return userBundle;
    }
    
    private PrimaryBlock makePrimaryBlock(
        DTNEndpointID recipient, PrimaryBlock.PriorityClass priorityClass,
        PrimaryBlock.LifeTime lifeTime
    ) {
        PrimaryBlock primaryBlock = new PrimaryBlock();
        
        primaryBlock.bundleProcessingControlFlags = makeBundlePCFs(priorityClass);
        primaryBlock.bundleID = DTNBundleID.from(daemon.getThisNodezEID(), Instant.now());
        primaryBlock.lifeTime = PrimaryBlock.LifeTime.setLifeTime(lifeTime);
        primaryBlock.destinationEID = recipient;
        primaryBlock.custodianEID = DTNEndpointID.from(primaryBlock.bundleID.sourceEID);
        primaryBlock.reportToEID = DTNEndpointID.from(primaryBlock.bundleID.sourceEID);
        primaryBlock.detailsIfFragment = new HashMap<>();
        
        return primaryBlock;
    }
    
    private BigInteger makeBundlePCFs(PrimaryBlock.PriorityClass priorityClass) {
        
        return PrimaryBlock.PriorityClass
            .setPriorityClass(BigInteger.ZERO, priorityClass)
            .setBit(PrimaryBlock.BundlePCF.BUNDLE_CUSTODY_TRANSFER_REQUESTED)
            .setBit(PrimaryBlock.BundlePCF.DESTINATION_ENDPOINT_IS_SINGLETON)
            /*request for a SINGLE bundle delivery report a.k.a return-receipt
            (from destination only)
            here, report-to dtnEndpointID == source dtnEndpointID*/
            .setBit(PrimaryBlock.BundlePCF.BUNDLE_DELIVERY_REPORT_REQUESTED);
    }
    
    private CanonicalBlock makePayloadCBlock(byte[] message) {
        CanonicalBlock payloadCBlock = new CanonicalBlock();
        payloadCBlock.blockTypeSpecificDataFields = makePayloadADU(message);
        payloadCBlock.blockTypeCode = CanonicalBlock.TypeCode.PAYLOAD;
        payloadCBlock.blockProcessingControlFlags = BigInteger.ZERO.setBit(
            CanonicalBlock.BlockPCF.BLOCK_MUST_BE_REPLICATED_IN_ALL_FRAGMENTS
        );
        return payloadCBlock;
    }
    
    private PayloadADU makePayloadADU(byte[] message) {
        
        PayloadADU payload = new PayloadADU();
        payload.ADU = Arrays.copyOf(message, message.length);
        
        return payload;
    }
    
    @Override
    public void deliver(DTNBundle bundle) {
        byte[] message = null;
    
        CanonicalBlock payloadCBlock = bundle.canonicalBlocks.get(DTNBundle.CBlockNumber.PAYLOAD);
        if (payloadCBlock != null) {
            PayloadADU adu = (PayloadADU) payloadCBlock.blockTypeSpecificDataFields;
            message = adu.ADU;
        }
    
        PrimaryBlock primaryBlock = bundle.primaryBlock;
        DTNEndpointID src =  primaryBlock.bundleID.sourceEID;
        
        ui.onReceiveDTNMessage(message, src.ssp);
    }
    
    @Override
    public void notifyOutboundBundleReceived(String recipient) {
        ui.onOutboundBundleReceived(recipient);
    }
}