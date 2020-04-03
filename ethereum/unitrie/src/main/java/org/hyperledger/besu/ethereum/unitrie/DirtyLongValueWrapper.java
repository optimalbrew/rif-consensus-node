package org.hyperledger.besu.ethereum.unitrie;

public class DirtyLongValueWrapper extends ValueWrapper {
    public static ValueWrapper DIRTY = dirty();

    protected DirtyLongValueWrapper(final byte[] value, final byte[] hash, final int length) {
        super(value,hash,length);

    }
    private static ValueWrapper dirty() {
        return ValueWrapper.fromValue(new byte[]{});
    }
}
