package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.aa.admin.Daemon2AdminAA;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.daemon.AdminAA2Daemon;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.CanonicalBlock;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.CustodySignal;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundle;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundleID;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNEndpointID;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.StatusReport;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RadAdminAATest {
    private static Daemon2AdminAA radAdminAA;
    
    @BeforeClass
    public static void setUpClass() {
        radAdminAA = new RadAdminAA(
            new AdminAA2Daemon() {
    
            @Override
            public boolean isUs(DTNEndpointID eid) {
                return false;
            }
    
            @Override
            public boolean isForUs(DTNBundle bundle) {
                return false;
            }
    
            @Override
            public boolean isFromUs(DTNBundle bundle) {
                return false;
            }
    
            @Override
            public void transmit(DTNBundle adminRecord) {
        
            }
    
            @Override
            public void notifyBundleStatus(String recipient, String msg) {
        
            }
    
            @Override
            public void delete(DTNBundleID bundleID) {
        
            }
            
            @Override
            public void delete(DTNBundleID bundleID, int fragmentOffset) {
        
            }
    
            @Override
            public DTNEndpointID getThisNodezEID() {
                return TestUtilities.makeDTNEID();
            }
        }, InstrumentationRegistry.getTargetContext()
        );
    }
    
    @Test
    public void testMakingCustodySignalForNonFragment() {
        DTNBundle testUserBundle = TestUtilities.generateNonFragmentBundle(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes()
        );
        
        DTNBundle custodySignal = radAdminAA.makeCustodySignal(
            testUserBundle, true, CustodySignal.Reason.NO_OTHER_INFO
        );
        
        CanonicalBlock adminCBlock
            = custodySignal.canonicalBlocks.get(DTNBundle.CBlockNumber.ADMIN_RECORD);
        
        assertNotNull(adminCBlock);
        
        assertTrue(adminCBlock.blockTypeSpecificDataFields instanceof CustodySignal);
        
        CustodySignal signal = (CustodySignal) adminCBlock.blockTypeSpecificDataFields;
    
        assertEquals(CustodySignal.RecordType.CUSTODY_SIGNAL, signal.recordType);
        assertEquals(testUserBundle.primaryBlock.bundleID, signal.subjectBundleID);
    
        assertFalse(signal.isForAFragment);
        assertNotNull(signal.detailsIfForAFragment);
        assertEquals(0, signal.detailsIfForAFragment.size());
        
        assertTrue(signal.custodyTransferSucceeded);
        
        assertEquals(CustodySignal.Reason.NO_OTHER_INFO, signal.reasonCode);
    }
    
    @Test
    public void testMakingCustodySignalForFragment() {
        DTNBundle testUserBundle = TestUtilities.createTestUserBundle(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes()
        );
    
        DTNBundle custodySignal = radAdminAA.makeCustodySignal(
            testUserBundle, true, CustodySignal.Reason.NO_OTHER_INFO
        );
    
        CanonicalBlock adminCBlock
            = custodySignal.canonicalBlocks.get(DTNBundle.CBlockNumber.ADMIN_RECORD);
    
        assertNotNull(adminCBlock);
    
        assertTrue(adminCBlock.blockTypeSpecificDataFields instanceof CustodySignal);
    
        CustodySignal signal = (CustodySignal) adminCBlock.blockTypeSpecificDataFields;
    
        assertEquals(CustodySignal.RecordType.CUSTODY_SIGNAL, signal.recordType);
        assertEquals(testUserBundle.primaryBlock.bundleID, signal.subjectBundleID);
        
        assertTrue(signal.isForAFragment);
        assertNotNull(signal.detailsIfForAFragment);
        assertTrue(signal.detailsIfForAFragment.size() > 0);
        
        assertTrue(signal.custodyTransferSucceeded);
    
        assertEquals(CustodySignal.Reason.NO_OTHER_INFO, signal.reasonCode);
    }
    
    @Test
    public void testMakingStatusReportForNonFragment() {
        DTNBundle testUserBundle = TestUtilities.generateNonFragmentBundle(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes()
        );
        
        DTNBundle statusReport = radAdminAA.makeStatusReport(
            testUserBundle, StatusReport.StatusFlags.BUNDLE_DELIVERED,
            StatusReport.Reason.NO_OTHER_INFO
        );
        
        CanonicalBlock adminCBlock
            = statusReport.canonicalBlocks.get(DTNBundle.CBlockNumber.ADMIN_RECORD);
        
        assertNotNull(adminCBlock);
        
        assertTrue(adminCBlock.blockTypeSpecificDataFields instanceof StatusReport);
        
        StatusReport report = (StatusReport) adminCBlock.blockTypeSpecificDataFields;
        
        assertEquals(StatusReport.RecordType.STATUS_REPORT, report.recordType);
        assertEquals(testUserBundle.primaryBlock.bundleID, report.subjectBundleID);
    
        assertFalse(report.isForAFragment);
        assertNotNull(report.detailsIfForAFragment);
        assertEquals(0, report.detailsIfForAFragment.size());
        
        assertEquals(StatusReport.StatusFlags.BUNDLE_DELIVERED, report.status);
//        assertNotNull(report.timeOfStatus);
//        assertTrue(report.timeOfStatus.compareTo(DTNTimeInstant.ZERO) > 0);
        assertEquals(StatusReport.Reason.NO_OTHER_INFO, report.reasonCode);
    }
    
    @Test
    public void testMakingStatusReportForFragment() {
        DTNBundle testUserBundle = TestUtilities.createTestUserBundle(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes()
        );
    
        DTNBundle statusReport = radAdminAA.makeStatusReport(
            testUserBundle, StatusReport.StatusFlags.BUNDLE_DELIVERED,
            StatusReport.Reason.NO_OTHER_INFO
        );
    
        CanonicalBlock adminCBlock
            = statusReport.canonicalBlocks.get(DTNBundle.CBlockNumber.ADMIN_RECORD);
    
        assertNotNull(adminCBlock);
    
        assertTrue(adminCBlock.blockTypeSpecificDataFields instanceof StatusReport);
    
        StatusReport report = (StatusReport) adminCBlock.blockTypeSpecificDataFields;
    
        assertEquals(StatusReport.RecordType.STATUS_REPORT, report.recordType);
        assertEquals(testUserBundle.primaryBlock.bundleID, report.subjectBundleID);
    
        assertTrue(report.isForAFragment);
        assertNotNull(report.detailsIfForAFragment);
        assertTrue(report.detailsIfForAFragment.size() > 0);
    
        assertEquals(StatusReport.StatusFlags.BUNDLE_DELIVERED, report.status);
//        assertNotNull(report.timeOfStatus);
//        assertTrue(report.timeOfStatus.compareTo(DTNTimeInstant.ZERO) > 0);
        assertEquals(StatusReport.Reason.NO_OTHER_INFO, report.reasonCode);
    }
    
    @Test
    public void testFakeStatusReportInputs() {
        DTNBundle testUserBundle = TestUtilities.createTestUserBundle(
            TestUtilities.TEST_SHORT_TEXT_MESSAGE.getBytes()
        );
    
        DTNBundle statusReport = radAdminAA.makeStatusReport(
            testUserBundle, 9, StatusReport.Reason.LIFETIME_EXPIRED
        );
    
        CanonicalBlock adminCBlock
            = statusReport.canonicalBlocks.get(DTNBundle.CBlockNumber.ADMIN_RECORD);
    
        assertNotNull(adminCBlock);
    
        assertTrue(adminCBlock.blockTypeSpecificDataFields instanceof StatusReport);
    
        StatusReport report = (StatusReport) adminCBlock.blockTypeSpecificDataFields;
    
        assertEquals(StatusReport.StatusFlags.INVALID_FLAG_SET, report.status);
        assertEquals(report.reasonCode, StatusReport.Reason.NO_OTHER_INFO);
    }
    
    @AfterClass
    public static void tearDownClass() {
        radAdminAA = null;
    }
}
