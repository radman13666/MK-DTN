package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.app.DTNUI;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.daemon.AppAA2Daemon;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.CanonicalBlock;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundle;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNEndpointID;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PayloadADU;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PrimaryBlock;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RadAppAATest {
    
    // manual mock objects
    private static final AppAA2Daemon daemon = new AppAA2Daemon() {
        @Override
        public void transmit(DTNBundle bundle) {
            assertFalse(bundle.primaryBlock.bundleProcessingControlFlags.testBit(
                    PrimaryBlock.BundlePCF.BUNDLE_IS_A_FRAGMENT
                )
            );
            
            CanonicalBlock payloadCBlock
                = bundle.canonicalBlocks.get(DTNBundle.CBlockNumber.PAYLOAD);
            assertNotNull(payloadCBlock);
            
            PayloadADU adu = (PayloadADU) payloadCBlock.blockTypeSpecificDataFields;
            assertNotNull(adu.ADU);
            
            String text = new String(adu.ADU);
            assertEquals(TestUtilities.TEST_SHORT_TEXT_MESSAGE, text);
    
            assertEquals(TestUtilities.TEST_LIFETIME, bundle.primaryBlock.lifeTime);
    
            assertEquals(TestUtilities.TEST_PRIORITY, PrimaryBlock.PriorityClass.getPriorityClass(
                bundle.primaryBlock.bundleProcessingControlFlags));
        }
    
        @Override
        public DTNEndpointID getThisNodezEID() {
            // short IDs for testing purposes only
            String eid = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
            return DTNEndpointID.from(DTNEndpointID.DTN_SCHEME, eid.substring(0, 8));
        }
    };
    
    private static final DTNUI ui = new DTNUI() {
        @Override
        public void onReceiveDTNMessage(byte[] message, String sender) {
            String text = new String(message);
            assertEquals(TestUtilities.TEST_SHORT_TEXT_MESSAGE, text);
            
            assertEquals(TestUtilities.TEST_SENDER, sender);
        }
    
        @Override
        public void onOutboundBundleReceived(String recipient) {
        
        }
    };
    
    private static RadAppAA appAA;
    
    @BeforeClass
    public static void setUp() {
        appAA = new RadAppAA(ui, daemon);
    }
    
    @Test
    public void testSending() {
        appAA.send(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes(),
            TestUtilities.TEST_RECIPIENT,
            TestUtilities.TEST_PRIORITY,
            PrimaryBlock.LifeTime.THREE_DAYS
        );
    }
    
    @Test
    public void testDeliveringBundle() {
        DTNBundle testBundle = TestUtilities.createTestUserBundle(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes()
        );
        appAA.deliver(testBundle);
    }
    
    @AfterClass
    public static void tearDown() {
        appAA = null;
    }
}