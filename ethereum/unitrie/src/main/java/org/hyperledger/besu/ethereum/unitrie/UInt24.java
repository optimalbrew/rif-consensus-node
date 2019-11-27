package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Type representing a 24 bit (3 bytes) value.
 */
public class UInt24 implements Comparable<UInt24> {

    private static final int MAX_INTEGER_VALUE = 0x00ffffff;

    public static final int BITS = 24;
    public static final int BYTES = BITS / Byte.SIZE;

    public static final UInt24 ZERO = new UInt24(0);
    public static final UInt24 MAX_VALUE = new UInt24(MAX_INTEGER_VALUE);

    private final int intValue;

    public static UInt24 fromBytesValue(final BytesValue value) {
        return fromBytesValue(value, 0);
    }

    public static UInt24 fromBytesValue(final BytesValue value, final int offset) {
        int intValue = (value.get(offset + 2) & 0xff) +
                ((value.get(offset + 1) & 0xff) << 8) +
                ((value.get(offset + 0) & 0xff) << 16);
        return new UInt24(intValue);
    }

    public UInt24(final int intValue) {
        if (intValue < 0 || intValue > MAX_INTEGER_VALUE) {
            throw new IllegalArgumentException("The supplied value doesn't fit in a Bytes24 instance");
        }
        this.intValue = intValue;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[BYTES];
        bytes[2] = (byte) (intValue);
        bytes[1] = (byte) (intValue >>>  8);
        bytes[0] = (byte) (intValue >>> 16);
        return bytes;
    }

    public int toInt() {
        return intValue;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        UInt24 bytes24 = (UInt24) other;
        return intValue == bytes24.intValue;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(intValue);
    }

    @Override
    public int compareTo(final UInt24 other) {
        return Integer.compare(intValue, other.intValue);
    }

    @Override
    public String toString() {
        return Integer.toString(intValue);
    }
}
