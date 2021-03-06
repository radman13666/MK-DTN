package org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn;

import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.CanonicalBlock;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundle;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.DTNBundleID;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PayloadADU;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.dto.PrimaryBlock;
import org.radindustries.radwolfsdragon.examples.wifip2ppeerdiscoverytest.dtn.fragmentmanager.Daemon2FragmentManager;

import java.math.BigInteger;
import java.util.Arrays;

final class RadFragMgr implements Daemon2FragmentManager {
    
    RadFragMgr() {}
    
    @Override
    public synchronized DTNBundle[] fragment(DTNBundle bundleToFragment) {
        return fragment(bundleToFragment, DEFAULT_FRAGMENT_PAYLOAD_SIZE_IN_BYTES);
    }
    
    @Override
    public synchronized DTNBundle[] fragment(
        DTNBundle bundleToFragment, int fragmentPayloadSizeInBytes
    ) {
        if (!DTNUtils.isUserData(bundleToFragment)) return new DTNBundle[]{bundleToFragment};
        
        if (bundleToFragment.primaryBlock.bundleProcessingControlFlags
            .testBit(PrimaryBlock.BundlePCF.BUNDLE_MUST_NOT_BE_FRAGMENTED) ||
            bundleToFragment.primaryBlock.bundleProcessingControlFlags
                .testBit(PrimaryBlock.BundlePCF.BUNDLE_IS_A_FRAGMENT))
            return new DTNBundle[]{bundleToFragment};
        
        CanonicalBlock cBlock
            = bundleToFragment.canonicalBlocks.get(DTNBundle.CBlockNumber.PAYLOAD);
        if (cBlock == null) return new DTNBundle[]{bundleToFragment};
        
        PayloadADU adu = (PayloadADU) cBlock.blockTypeSpecificDataFields;
        int originalPayloadSizeInBytes = adu.ADU.length;
        
        if (fragmentPayloadSizeInBytes >= originalPayloadSizeInBytes)
            return new DTNBundle[]{bundleToFragment};
        else if (fragmentPayloadSizeInBytes <= 0)
            return new DTNBundle[0];
        
        int numFullFragments
            = originalPayloadSizeInBytes / fragmentPayloadSizeInBytes;
        
        DTNBundle[] fragments = new DTNBundle[numFullFragments + 1];
        
        for (int m = 0, n = 1;
             m < numFullFragments && n < numFullFragments + 1;
             m++, n++) {
            fragments[m]
                = makeFragment(
                bundleToFragment, m, originalPayloadSizeInBytes,
                m * fragmentPayloadSizeInBytes,
                (n * fragmentPayloadSizeInBytes) - 1
            );
        }
        
        fragments[numFullFragments]
            = makeFragment(
            bundleToFragment, numFullFragments, originalPayloadSizeInBytes,
            numFullFragments * fragmentPayloadSizeInBytes,
            originalPayloadSizeInBytes - 1
        );
        
        return fragments;
    }
    
    private synchronized DTNBundle makeFragment(
        DTNBundle bundleToFragment, int fragmentOffset, int totalADULength, int start, int stop
    ) {
        PrimaryBlock fragmentPrimaryBlock = generateFragmentPrimaryBlock(
            bundleToFragment, fragmentOffset, totalADULength
        );
        
        CanonicalBlock fragmentPayloadCanonicalBlock
            = generateFragmentPayloadBlock(bundleToFragment, start, stop);
        
        DTNBundle fragment = new DTNBundle();
        fragment.primaryBlock = fragmentPrimaryBlock;
        fragment.canonicalBlocks.put(
            DTNBundle.CBlockNumber.PAYLOAD, fragmentPayloadCanonicalBlock
        );
        
        return fragment;
    }
    
    private synchronized PrimaryBlock generateFragmentPrimaryBlock(
        DTNBundle bundleToFragment, int fragmentOffset, int totalADULength
    ) {
        PrimaryBlock fragmentPrimaryBlock = new PrimaryBlock();
        
        fragmentPrimaryBlock.bundleProcessingControlFlags = new BigInteger(
            bundleToFragment.primaryBlock.bundleProcessingControlFlags.toString()
        ).setBit(PrimaryBlock.BundlePCF.BUNDLE_IS_A_FRAGMENT)
            .setBit(PrimaryBlock.BundlePCF.BUNDLE_MUST_NOT_BE_FRAGMENTED);
        
        if (fragmentPrimaryBlock.bundleProcessingControlFlags
            .testBit(PrimaryBlock.BundlePCF.BUNDLE_IS_A_FRAGMENT)) {
            fragmentPrimaryBlock.detailsIfFragment.put(
                PrimaryBlock.FragmentField.FRAGMENT_OFFSET,
                Integer.toString(fragmentOffset)
            );
            fragmentPrimaryBlock.detailsIfFragment.put(
                PrimaryBlock.FragmentField.TOTAL_ADU_LENGTH,
                Integer.toString(totalADULength)
            );
        }
        
        fragmentPrimaryBlock.bundleID = DTNBundleID.from(
            bundleToFragment.primaryBlock.bundleID.sourceEID,
            bundleToFragment.primaryBlock.bundleID.creationTimestamp
        );
        
        fragmentPrimaryBlock.priorityClass = bundleToFragment.primaryBlock.priorityClass;
        
        fragmentPrimaryBlock.lifeTime = bundleToFragment.primaryBlock.lifeTime;
        
        fragmentPrimaryBlock.destinationEID
            = bundleToFragment.primaryBlock.destinationEID;
        
        fragmentPrimaryBlock.custodianEID
            = bundleToFragment.primaryBlock.custodianEID;
        
        fragmentPrimaryBlock.reportToEID
            = bundleToFragment.primaryBlock.reportToEID;
        
        return fragmentPrimaryBlock;
    }
    
    private synchronized CanonicalBlock generateFragmentPayloadBlock(
        DTNBundle bundleToFragment, int start, int stop
    ) {
        CanonicalBlock bundlePayloadCBlock = bundleToFragment.canonicalBlocks.get(
            DTNBundle.CBlockNumber.PAYLOAD);
        
        assert bundlePayloadCBlock != null;
        
        CanonicalBlock payloadCBlock = new CanonicalBlock();
        payloadCBlock.blockType = CanonicalBlock.BlockType.PAYLOAD;
        payloadCBlock.mustBeReplicatedInAllFragments
            = bundlePayloadCBlock.mustBeReplicatedInAllFragments;
        
        PayloadADU bundlePayload
            = (PayloadADU) bundlePayloadCBlock.blockTypeSpecificDataFields;
        
        PayloadADU fragADU = new PayloadADU();
        fragADU.ADU = Arrays.copyOfRange(bundlePayload.ADU, start, stop + 1);
        payloadCBlock.blockTypeSpecificDataFields = fragADU;
        
        return payloadCBlock;
    }
    
    @Override
    public synchronized DTNBundle defragment(DTNBundle[] fragmentsToCombine) {
        DTNBundle template = lastFragment(fragmentsToCombine);
        
        PrimaryBlock primaryBlock
            = generateDefragmentedBundlePrimaryBlock(template);
        
        CanonicalBlock payloadCBlock
            = generateDefragmentedBundlePayloadCBlock(fragmentsToCombine);
        
        DTNBundle originalBundle = new DTNBundle();
        originalBundle.primaryBlock = primaryBlock;
        originalBundle.canonicalBlocks.put(DTNBundle.CBlockNumber.PAYLOAD, payloadCBlock);
    
        return originalBundle;
    }
    
    private synchronized DTNBundle lastFragment(DTNBundle[] fragments) {
        int last = 0;
        for (DTNBundle fragment : fragments) {
            String fragmentOffset
                = fragment.primaryBlock.detailsIfFragment
                .get(PrimaryBlock.FragmentField.FRAGMENT_OFFSET);
            assert fragmentOffset != null;
            
            int offset = Integer.parseInt(fragmentOffset);
            if (offset > last) last = offset;
        }
        return fragments[last];
    }
    
    private synchronized PrimaryBlock generateDefragmentedBundlePrimaryBlock(DTNBundle fragment) {
        PrimaryBlock pbForOriginalBundle = new PrimaryBlock();
        PrimaryBlock fragPrimaryBlock = fragment.primaryBlock;
        
        pbForOriginalBundle.bundleID
            = DTNBundleID.from(
                fragPrimaryBlock.bundleID.sourceEID,
                fragPrimaryBlock.bundleID.creationTimestamp
            );
        
        pbForOriginalBundle.lifeTime = fragPrimaryBlock.lifeTime;
        pbForOriginalBundle.destinationEID = fragPrimaryBlock.destinationEID;
        pbForOriginalBundle.custodianEID = fragPrimaryBlock.custodianEID;
        pbForOriginalBundle.reportToEID = fragPrimaryBlock.reportToEID;
        pbForOriginalBundle.priorityClass = fragPrimaryBlock.priorityClass;
        
        pbForOriginalBundle.bundleProcessingControlFlags
            = new BigInteger(fragPrimaryBlock.bundleProcessingControlFlags.toString())
            .clearBit(PrimaryBlock.BundlePCF.BUNDLE_IS_A_FRAGMENT)
            .clearBit(PrimaryBlock.BundlePCF.BUNDLE_MUST_NOT_BE_FRAGMENTED);
        
        return pbForOriginalBundle;
    }
    
    private synchronized CanonicalBlock generateDefragmentedBundlePayloadCBlock(
        DTNBundle[] fragmentsToCombine
    ) {
        CanonicalBlock payloadCBlock = new CanonicalBlock();
        CanonicalBlock fragPayloadCBlock = fragmentsToCombine[0].canonicalBlocks
            .get(DTNBundle.CBlockNumber.PAYLOAD);
        
        assert fragPayloadCBlock != null;
        payloadCBlock.blockType = CanonicalBlock.BlockType.PAYLOAD;
        payloadCBlock.mustBeReplicatedInAllFragments
            = fragPayloadCBlock.mustBeReplicatedInAllFragments;
        payloadCBlock.blockTypeSpecificDataFields
            = combineFragmentADUs(fragmentsToCombine, fragPayloadCBlock);
        
        return payloadCBlock;
    }
    
    private synchronized PayloadADU combineFragmentADUs(
        DTNBundle[] fragmentsToCombine, CanonicalBlock firstFragPayloadCBlock
    ) {
        PayloadADU originalADU = new PayloadADU();
        PayloadADU firstsADU = (PayloadADU) firstFragPayloadCBlock.blockTypeSpecificDataFields;
        originalADU.ADU = firstsADU.ADU;
        
        for (int i = 1; i < fragmentsToCombine.length; i++) {
            CanonicalBlock payloadCBlock = fragmentsToCombine[i].canonicalBlocks
                .get(DTNBundle.CBlockNumber.PAYLOAD);
            
            assert payloadCBlock != null;
            PayloadADU fragADU = (PayloadADU) payloadCBlock.blockTypeSpecificDataFields;
            
            originalADU.ADU = concatenateData(originalADU.ADU, fragADU.ADU);
        }
        
        return originalADU;
    }
    
    private synchronized byte[] concatenateData(byte[] augend, byte[] addend) {
        int len = augend.length + addend.length;
        byte[] result = new byte[len];
        
        System.arraycopy(augend, 0, result, 0, augend.length);
        
        for (int j = augend.length, k = 0; j < len && k < addend.length; j++, k++) {
            result[j] = addend[k];
        }
        
        return result;
    }
    
    @Override
    public synchronized boolean defragmentable(DTNBundle[] fragmentsToCombine) {
        return fromSameDTNBundle(fragmentsToCombine)
            && matchSizeOfOriginalDTNBundle(fragmentsToCombine);
    }
    
    private synchronized boolean fromSameDTNBundle(DTNBundle[] fragments) {
        boolean result = true;
        DTNBundle first = fragments[0];
        
        for (DTNBundle fragment : fragments) {
            if (!(
                fragment.primaryBlock.bundleProcessingControlFlags
                    .testBit(PrimaryBlock.BundlePCF.BUNDLE_IS_A_FRAGMENT) //is a fragment
                    &&
                    fragment.primaryBlock.bundleID.sourceEID
                        .equals(first.primaryBlock.bundleID.sourceEID) //come from the same place
                    &&
                    fragment.primaryBlock.bundleID.creationTimestamp //made at the same time
                        == first.primaryBlock.bundleID.creationTimestamp
            )) {
                result = false;
                break;
            }
        }
        
        return result;
    }
    
    private synchronized boolean matchSizeOfOriginalDTNBundle(DTNBundle[] fragments) {
        
        String aduLength = fragments[0].primaryBlock.detailsIfFragment
            .get(PrimaryBlock.FragmentField.TOTAL_ADU_LENGTH);
        
        assert aduLength != null;
        int originalBundlezPayloadSizeInBytes = Integer.parseInt(aduLength);
        
        int totalFragmentPayloadSize = 0;
        
        for (DTNBundle fragment : fragments) {
            CanonicalBlock payloadCBlock = fragment.canonicalBlocks
                .get(DTNBundle.CBlockNumber.PAYLOAD);
            
            assert payloadCBlock != null;
            PayloadADU fragmentADU = (PayloadADU) payloadCBlock.blockTypeSpecificDataFields;
            
            totalFragmentPayloadSize += fragmentADU.ADU.length;
        }
        
        return totalFragmentPayloadSize == originalBundlezPayloadSizeInBytes;
    }
}
