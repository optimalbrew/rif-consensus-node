package co.rsk.utils;

import org.hyperledger.besu.util.bytes.BytesValue;

public abstract class ByteUtils {

    public static BytesValue stripLeadingZeroes(final BytesValue data) {

        BytesValue valueForZero = BytesValue.of(0x00);
        if (data == null) {
            return null;
        }

        final int firstNonZero = firstNonZeroByte(data.getArrayUnsafe());
        switch (firstNonZero) {
            case -1:
                return valueForZero;

            case 0:
                return data;

            default:
                byte[] result = new byte[data.size() - firstNonZero];
                data.copyTo(result, firstNonZero,0, data.size() - firstNonZero);
                return BytesValue.wrap(result);
        }
    }

    private static int firstNonZeroByte(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != 0) {
                return i;
            }
        }
        return -1;
    }
}
