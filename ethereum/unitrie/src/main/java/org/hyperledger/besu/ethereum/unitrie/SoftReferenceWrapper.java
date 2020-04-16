package org.hyperledger.besu.ethereum.unitrie;

import java.lang.ref.SoftReference;

public class SoftReferenceWrapper {
    public SoftReference<byte[]> value;

    public SoftReferenceWrapper(final byte[] aValue) {
        this.value = new SoftReference<>(aValue);
    }
}
