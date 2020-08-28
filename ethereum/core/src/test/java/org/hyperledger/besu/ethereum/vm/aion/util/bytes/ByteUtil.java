package org.hyperledger.besu.ethereum.vm.aion.util.bytes;

import org.hyperledger.besu.ethereum.vm.aion.util.conversions.Hex;
import org.hyperledger.besu.ethereum.vm.aion.util.conversions.HexEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ByteUtil {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private static final HexEncoder encoder = new HexEncoder();

    /**
     * Cast hex encoded value from byte[] to int
     *
     * <p>Limited to Integer.MAX_VALUE: 2^31-1 (4 bytes)
     *
     * @param b array contains the values
     * @return unsigned positive int value.
     */
    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0) {
            return 0;
        }
        return new BigInteger(1, b).intValue();
    }

    /**
     * @param arrays - arrays to merge
     * @return - merged array
     */
    public static byte[] merge(byte[]... arrays) {
        int count = 0;
        for (byte[] array : arrays) {
            count += array.length;
        }

        // Create new array and copy all array contents
        byte[] mergedArray = new byte[count];
        int start = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return mergedArray;
    }

    public static boolean isNullOrZeroArray(byte[] array) {
        return (array == null) || (array.length == 0);
    }

    public static boolean isSingleZero(byte[] array) {
        return (array.length == 1 && array[0] == 0);
    }

    /**
     * Converts a int value into a byte array.
     *
     * @param val - int value to convert
     * @return value with leading byte that are zeroes striped
     */
    public static byte[] intToBytesNoLeadZeroes(int val) {
        if (val == 0) {
            return EMPTY_BYTE_ARRAY;
        } else if (val < 0 || val > 16777215) {
            return new byte[] {
                    (byte) (val >>> 24), (byte) (val >>> 16), (byte) (val >>> 8), (byte) val
            };
        } else if (val < 256) {
            return new byte[] {(byte) val};
        } else if (val < 65536) {
            return new byte[] {(byte) (val >>> 8), (byte) val};
        } else {
            return new byte[] {(byte) (val >>> 16), (byte) (val >>> 8), (byte) val};
        }
    }

    /**
     * Omitting sign indication byte. <br>
     * <br>
     * Instead of {@code org.spongycastle.util.BigIntegers#asUnsignedByteArray(BigInteger)} <br>
     * we use this custom method to avoid an empty array in case of BigInteger.ZERO
     *
     * @param value - any big integer number. A <code>null</code>-value will return <code>null
     *     </code>
     * @return A byte array without a leading zero byte if present in the signed encoding.
     *     BigInteger.ZERO will return an array with length 1 and byte-value 0.
     */
    public static byte[] bigIntegerToBytes(BigInteger value) {
        if (value == null) {
            return null;
        }

        byte[] data = value.toByteArray();

        if (data.length != 1 && data[0] == 0) {
            byte[] tmp = new byte[data.length - 1];
            System.arraycopy(data, 1, tmp, 0, tmp.length);
            data = tmp;
        }
        return data;
    }

    /**
     * The regular {@link BigInteger#toByteArray()} method isn't quite what we often need: it
     * appends a leading zero to indicate that the number is positive and may need padding.
     *
     * @param b the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null) {
            return null;
        }
        byte[] bytes = new byte[numBytes];
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    /**
     * Convert a byte-array into a hex String.<br>
     * Works similar to {@link Hex#toHexString} but allows for <code>null</code>
     *
     * @param data - byte-array to convert to a hex-string
     * @return hex representation of the data.<br>
     *     Returns an empty String if the input is <code>null</code> TODO: swap out with more
     *     efficient implementation, for now seems like we are stuck with this
     * @see Hex#toHexString
     */
    public static String toHexString(byte[] data) {
        return data == null ? "" : Hex.toHexString(data);
    }

    public static byte[] hexStringToBytes(String data) {
        if (data == null) {
            return EMPTY_BYTE_ARRAY;
        }
        if (data.startsWith("0x")) {
            data = data.substring(2);
        }
        if ((data.length() & 1) == 1) {
            data = "0" + data;
        }
        return decode(data);
    }

    private static byte[] decode(String data) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try {
            encoder.decode(data, bOut);
        } catch (IOException e) {
            System.err.println("Hex decode failed! " + data);
            return null;
        }
        return bOut.toByteArray();
    }
}
