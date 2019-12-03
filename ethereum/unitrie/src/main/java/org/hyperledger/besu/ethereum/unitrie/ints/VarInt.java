package org.hyperledger.besu.ethereum.unitrie.ints;

/**
 * A variable-length encoded unsigned integer using Satoshi's encoding (a.k.a. "CompactSize").
 */
public class VarInt {
    private final long value;
    private final int originallyEncodedSize;

    /**
     * Constructs a new VarInt with the given unsigned long value.
     *
     * @param value the unsigned long value (beware widening conversion of negatives!)
     */
    public VarInt(final long value) {
        this.value = value;
        originallyEncodedSize = getSizeInBytes();
    }

    /**
     * Constructs a new VarInt with the value parsed from the specified offset of the given buffer.
     *
     * @param buf the buffer containing the value
     * @param offset the offset of the value
     */
    public VarInt(final byte[] buf, final int offset) {
        int first = 0xFF & buf[offset];
        if (first < 253) {
            value = first;
            originallyEncodedSize = 1; // 1 data byte (8 bits)
        } else if (first == 253) {
            value = (0xFF & buf[offset + 1]) | ((0xFF & buf[offset + 2]) << 8);
            originallyEncodedSize = 3; // 1 marker + 2 data bytes (16 bits)
        } else if (first == 254) {
            value = readUint32(buf, offset + 1);
            originallyEncodedSize = 5; // 1 marker + 4 data bytes (32 bits)
        } else {
            value = readInt64(buf, offset + 1);
            originallyEncodedSize = 9; // 1 marker + 8 data bytes (64 bits)
        }
    }

    /**
     * Returns this instance value.
     */
    public long getValue() {
        return value;
    }

    /**
     * Returns the original number of bytes used to encode the value if it was
     * deserialized from a byte array, or the minimum encoded size if it was not.
     */
    public int getOriginalSizeInBytes() {
        return originallyEncodedSize;
    }

    /**
     * Returns the minimum encoded size of the value.
     */
    public final int getSizeInBytes() {
        return sizeOf(value);
    }

    /**
     * Returns the minimum encoded size of the given unsigned long value.
     *
     * @param value the unsigned long value (beware widening conversion of negatives!)
     */
    public static int sizeOf(final long value) {
        // if negative, it's actually a very large unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }

    /**
     * Encodes the value into its minimal representation.
     *
     * @return the minimal encoded bytes of the value
     */
    public byte[] encode() {
        byte[] bytes;
        switch (sizeOf(value)) {
            case 1:
                return new byte[]{(byte) value};
            case 3:
                return new byte[]{(byte) 253, (byte) (value), (byte) (value >> 8)};
            case 5:
                bytes = new byte[5];
                bytes[0] = (byte) 254;
                uint32ToByteArrayLE(value, bytes, 1);
                return bytes;
            default:
                bytes = new byte[9];
                bytes[0] = (byte) 255;
                uint64ToByteArrayLE(value, bytes, 1);
                return bytes;
        }
    }

    private long readUint32(final byte[] bytes, final int offset) {
        return (bytes[offset] & 0xffL) |
                ((bytes[offset + 1] & 0xffL) << 8) |
                ((bytes[offset + 2] & 0xffL) << 16) |
                ((bytes[offset + 3] & 0xffL) << 24);
    }

    private long readInt64(final byte[] bytes, final int offset) {
        return (bytes[offset] & 0xffL) |
                ((bytes[offset + 1] & 0xffL) << 8) |
                ((bytes[offset + 2] & 0xffL) << 16) |
                ((bytes[offset + 3] & 0xffL) << 24) |
                ((bytes[offset + 4] & 0xffL) << 32) |
                ((bytes[offset + 5] & 0xffL) << 40) |
                ((bytes[offset + 6] & 0xffL) << 48) |
                ((bytes[offset + 7] & 0xffL) << 56);
    }

    private void uint32ToByteArrayLE(final long val, final byte[] out, final int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    private void uint64ToByteArrayLE(final long val, final byte[] out, final int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));
    }
}