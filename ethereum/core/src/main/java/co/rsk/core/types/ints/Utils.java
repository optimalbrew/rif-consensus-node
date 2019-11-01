package co.rsk.core.types.ints;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Ordering;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Collections;
import java.util.Date;
//import java.util.Iterator;
//import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
//import org.spongycastle.crypto.digests.RIPEMD160Digest;

public class Utils {
    public static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
    public static final byte[] BITCOIN_SIGNED_MESSAGE_HEADER_BYTES;
    private static final Joiner SPACE_JOINER;
    public static final int MAX_INITIAL_ARRAY_LENGTH = 20;
    private static BlockingQueue<Boolean> mockSleepQueue;
    public static final BaseEncoding HEX;
    public static volatile Date mockTime;
    private static final TimeZone UTC;
    private static final int[] bitMask;
    private static int isAndroid;

    public Utils() {
    }

    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null) {
            return null;
        } else {
            byte[] bytes = new byte[numBytes];
            byte[] biBytes = b.toByteArray();
            int start = biBytes.length == numBytes + 1 ? 1 : 0;
            int length = Math.min(biBytes.length, numBytes);
            System.arraycopy(biBytes, start, bytes, numBytes - length, length);
            return bytes;
        }
    }

    public static void uint32ToByteArrayBE(long val, byte[] out, int offset) {
        out[offset] = (byte)((int)(255L & val >> 24));
        out[offset + 1] = (byte)((int)(255L & val >> 16));
        out[offset + 2] = (byte)((int)(255L & val >> 8));
        out[offset + 3] = (byte)((int)(255L & val));
    }

    public static void uint32ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte)((int)(255L & val));
        out[offset + 1] = (byte)((int)(255L & val >> 8));
        out[offset + 2] = (byte)((int)(255L & val >> 16));
        out[offset + 3] = (byte)((int)(255L & val >> 24));
    }

    public static void uint64ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte)((int)(255L & val));
        out[offset + 1] = (byte)((int)(255L & val >> 8));
        out[offset + 2] = (byte)((int)(255L & val >> 16));
        out[offset + 3] = (byte)((int)(255L & val >> 24));
        out[offset + 4] = (byte)((int)(255L & val >> 32));
        out[offset + 5] = (byte)((int)(255L & val >> 40));
        out[offset + 6] = (byte)((int)(255L & val >> 48));
        out[offset + 7] = (byte)((int)(255L & val >> 56));
    }

    public static void uint32ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int)(255L & val));
        stream.write((int)(255L & val >> 8));
        stream.write((int)(255L & val >> 16));
        stream.write((int)(255L & val >> 24));
    }

    public static void int64ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int)(255L & val));
        stream.write((int)(255L & val >> 8));
        stream.write((int)(255L & val >> 16));
        stream.write((int)(255L & val >> 24));
        stream.write((int)(255L & val >> 32));
        stream.write((int)(255L & val >> 40));
        stream.write((int)(255L & val >> 48));
        stream.write((int)(255L & val >> 56));
    }

    public static void uint64ToByteStreamLE(BigInteger val, OutputStream stream) throws IOException {
        byte[] bytes = val.toByteArray();
        if (bytes.length > 8) {
            throw new RuntimeException("Input too large to encode into a uint64");
        } else {
            bytes = reverseBytes(bytes);
            stream.write(bytes);
            if (bytes.length < 8) {
                for(int i = 0; i < 8 - bytes.length; ++i) {
                    stream.write(0);
                }
            }

        }
    }

    public static boolean isLessThanUnsigned(long n1, long n2) {
        return UnsignedLongs.compare(n1, n2) < 0;
    }

    public static boolean isLessThanOrEqualToUnsigned(long n1, long n2) {
        return UnsignedLongs.compare(n1, n2) <= 0;
    }

    public static byte[] reverseBytes(byte[] bytes) {
        byte[] buf = new byte[bytes.length];

        for(int i = 0; i < bytes.length; ++i) {
            buf[i] = bytes[bytes.length - 1 - i];
        }

        return buf;
    }

    public static byte[] reverseDwordBytes(byte[] bytes, int trimLength) {
        Preconditions.checkArgument(bytes.length % 4 == 0);
        Preconditions.checkArgument(trimLength < 0 || trimLength % 4 == 0);
        byte[] rev = new byte[trimLength >= 0 && bytes.length > trimLength ? trimLength : bytes.length];

        for(int i = 0; i < rev.length; i += 4) {
            System.arraycopy(bytes, i, rev, i, 4);

            for(int j = 0; j < 4; ++j) {
                rev[i + j] = bytes[i + 3 - j];
            }
        }

        return rev;
    }

    public static long readUint32(byte[] bytes, int offset) {
        return (long)bytes[offset] & 255L | ((long)bytes[offset + 1] & 255L) << 8 | ((long)bytes[offset + 2] & 255L) << 16 | ((long)bytes[offset + 3] & 255L) << 24;
    }

    public static long readInt64(byte[] bytes, int offset) {
        return (long)bytes[offset] & 255L | ((long)bytes[offset + 1] & 255L) << 8 | ((long)bytes[offset + 2] & 255L) << 16 | ((long)bytes[offset + 3] & 255L) << 24 | ((long)bytes[offset + 4] & 255L) << 32 | ((long)bytes[offset + 5] & 255L) << 40 | ((long)bytes[offset + 6] & 255L) << 48 | ((long)bytes[offset + 7] & 255L) << 56;
    }

    public static long readUint32BE(byte[] bytes, int offset) {
        return ((long)bytes[offset] & 255L) << 24 | ((long)bytes[offset + 1] & 255L) << 16 | ((long)bytes[offset + 2] & 255L) << 8 | (long)bytes[offset + 3] & 255L;
    }

    public static int readUint16BE(byte[] bytes, int offset) {
        return (bytes[offset] & 255) << 8 | bytes[offset + 1] & 255;
    }

    /*public static byte[] sha256hash160(byte[] input) {
        byte[] sha256 = Sha256Hash.hash(input);
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(sha256, 0, sha256.length);
        byte[] out = new byte[20];
        digest.doFinal(out, 0);
        return out;
    }*/

    public static BigInteger decodeMPI(byte[] mpi, boolean hasLength) {
        byte[] buf;
        if (hasLength) {
            int length = (int)readUint32BE(mpi, 0);
            buf = new byte[length];
            System.arraycopy(mpi, 4, buf, 0, length);
        } else {
            buf = mpi;
        }

        if (buf.length == 0) {
            return BigInteger.ZERO;
        } else {
            boolean isNegative = (buf[0] & 128) == 128;
            if (isNegative) {
                buf[0] = (byte)(buf[0] & 127);
            }

            BigInteger result = new BigInteger(buf);
            return isNegative ? result.negate() : result;
        }
    }

    public static byte[] encodeMPI(BigInteger value, boolean includeLength) {
        if (value.equals(BigInteger.ZERO)) {
            return !includeLength ? new byte[0] : new byte[]{0, 0, 0, 0};
        } else {
            boolean isNegative = value.signum() < 0;
            if (isNegative) {
                value = value.negate();
            }

            byte[] array = value.toByteArray();
            int length = array.length;
            if ((array[0] & 128) == 128) {
                ++length;
            }

            byte[] result;
            if (includeLength) {
                result = new byte[length + 4];
                System.arraycopy(array, 0, result, length - array.length + 3, array.length);
                uint32ToByteArrayBE((long)length, result, 0);
                if (isNegative) {
                    result[4] = (byte)(result[4] | 128);
                }

                return result;
            } else {
                if (length != array.length) {
                    result = new byte[length];
                    System.arraycopy(array, 0, result, 1, array.length);
                } else {
                    result = array;
                }

                if (isNegative) {
                    result[0] = (byte)(result[0] | 128);
                }

                return result;
            }
        }
    }

    public static BigInteger decodeCompactBits(long compact) {
        int size = (int)(compact >> 24) & 255;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte)size;
        if (size >= 1) {
            bytes[4] = (byte)((int)(compact >> 16 & 255L));
        }

        if (size >= 2) {
            bytes[5] = (byte)((int)(compact >> 8 & 255L));
        }

        if (size >= 3) {
            bytes[6] = (byte)((int)(compact & 255L));
        }

        return decodeMPI(bytes, true);
    }

    public static long encodeCompactBits(BigInteger value) {
        int size = value.toByteArray().length;
        long result;
        if (size <= 3) {
            result = value.longValue() << 8 * (3 - size);
        } else {
            result = value.shiftRight(8 * (size - 3)).longValue();
        }

        if ((result & 8388608L) != 0L) {
            result >>= 8;
            ++size;
        }

        result |= (long)(size << 24);
        result |= value.signum() == -1 ? 8388608L : 0L;
        return result;
    }

    public static Date rollMockClock(int seconds) {
        return rollMockClockMillis((long)(seconds * 1000));
    }

    public static Date rollMockClockMillis(long millis) {
        if (mockTime == null) {
            throw new IllegalStateException("You need to use setMockClock() first.");
        } else {
            mockTime = new Date(mockTime.getTime() + millis);
            return mockTime;
        }
    }

    public static void setMockClock() {
        mockTime = new Date();
    }

    public static void setMockClock(long mockClockSeconds) {
        mockTime = new Date(mockClockSeconds * 1000L);
    }

    public static Date now() {
        return mockTime != null ? mockTime : new Date();
    }

    public static long currentTimeMillis() {
        return mockTime != null ? mockTime.getTime() : System.currentTimeMillis();
    }

    public static long currentTimeSeconds() {
        return currentTimeMillis() / 1000L;
    }

    public static String dateTimeFormat(Date dateTime) {
        DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601.setTimeZone(UTC);
        return iso8601.format(dateTime);
    }

    public static String dateTimeFormat(long dateTime) {
        DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601.setTimeZone(UTC);
        return iso8601.format(dateTime);
    }

    public static <T> String join(Iterable<T> items) {
        return SPACE_JOINER.join(items);
    }

    public static byte[] copyOf(byte[] in, int length) {
        byte[] out = new byte[length];
        System.arraycopy(in, 0, out, 0, Math.min(length, in.length));
        return out;
    }

    public static byte[] appendByte(byte[] bytes, byte b) {
        byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
        result[result.length - 1] = b;
        return result;
    }

    public static String toString(byte[] bytes, String charsetName) {
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException(var3);
        }
    }

    public static byte[] toBytes(CharSequence str, String charsetName) {
        try {
            return str.toString().getBytes(charsetName);
        } catch (UnsupportedEncodingException var3) {
            throw new RuntimeException(var3);
        }
    }

    /*public static byte[] parseAsHexOrBase58(String data) {
        try {
            return HEX.decode(data);
        } catch (Exception var4) {
            try {
                return Base58.decodeChecked(data);
            } catch (AddressFormatException var3) {
                return null;
            }
        }
    }*/

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static byte[] formatMessageForSigning(String message) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.length);
            bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES);
            byte[] messageBytes = message.getBytes(Charsets.UTF_8);
            VarInt size = new VarInt((long)messageBytes.length);
            bos.write(size.encode());
            bos.write(messageBytes);
            return bos.toByteArray();
        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    public static boolean checkBitLE(byte[] data, int index) {
        return (data[index >>> 3] & bitMask[7 & index]) != 0;
    }

    public static void setBitLE(byte[] data, int index) {
        data[index >>> 3] = (byte)(data[index >>> 3] | bitMask[7 & index]);
    }

    public static void sleep(long millis) {
        if (mockSleepQueue == null) {
            Uninterruptibles.sleepUninterruptibly(millis, TimeUnit.MILLISECONDS);
        } else {
            try {
                boolean isMultiPass = (Boolean)mockSleepQueue.take();
                rollMockClockMillis(millis);
                if (isMultiPass) {
                    mockSleepQueue.offer(true);
                }
            } catch (InterruptedException var3) {
            }
        }

    }

    public static void setMockSleep(boolean isEnable) {
        if (isEnable) {
            mockSleepQueue = new ArrayBlockingQueue(1);
            mockTime = new Date(System.currentTimeMillis());
        } else {
            mockSleepQueue = null;
        }

    }

    public static void passMockSleep() {
        mockSleepQueue.offer(false);
    }

    public static void finishMockSleep() {
        if (mockSleepQueue != null) {
            mockSleepQueue.offer(true);
        }

    }

    public static boolean isAndroidRuntime() {
        if (isAndroid == -1) {
            String runtime = System.getProperty("java.runtime.name");
            isAndroid = runtime != null && runtime.equals("Android Runtime") ? 1 : 0;
        }

        return isAndroid == 1;
    }

   /* public static int maxOfMostFreq(int... items) {
        ArrayList<Integer> list = new ArrayList(items.length);
        int[] var2 = items;
        int var3 = items.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            int item = var2[var4];
            list.add(item);
        }

        return maxOfMostFreq((List)list);
    }*/

   /* public static int maxOfMostFreq(List<Integer> items) {
        if (items.isEmpty()) {
            return 0;
        } else {
            items = Ordering.natural().reverse().sortedCopy(items);
            LinkedList<Utils.Pair> pairs = Lists.newLinkedList();
            pairs.add(new Utils.Pair((Integer)items.get(0), 0));

            int maxItem;
            Utils.Pair pair;
            for(Iterator var2 = items.iterator(); var2.hasNext(); ++pair.count) {
                maxItem = (Integer)var2.next();
                pair = (Utils.Pair)pairs.getLast();
                if (pair.item != maxItem) {
                    pairs.add(pair = new Utils.Pair(maxItem, 0));
                }
            }

            Collections.sort(pairs);
            int maxCount = ((Utils.Pair)pairs.getFirst()).count;
            maxItem = ((Utils.Pair)pairs.getFirst()).item;

            Utils.Pair pair;
            for(Iterator var7 = pairs.iterator(); var7.hasNext(); maxItem = Math.max(maxItem, pair.item)) {
                pair = (Utils.Pair)var7.next();
                if (pair.count != maxCount) {
                    break;
                }
            }

            return maxItem;
        }
    }*/

    public static String getResourceAsString(URL url) throws IOException {
        List<String> lines = Resources.readLines(url, Charsets.UTF_8);
        return Joiner.on('\n').join(lines);
    }

    public static InputStream closeUnchecked(InputStream stream) {
        try {
            stream.close();
            return stream;
        } catch (IOException var2) {
            throw new RuntimeException(var2);
        }
    }

    public static OutputStream closeUnchecked(OutputStream stream) {
        try {
            stream.close();
            return stream;
        } catch (IOException var2) {
            throw new RuntimeException(var2);
        }
    }

    static {
        BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = "Bitcoin Signed Message:\n".getBytes(Charsets.UTF_8);
        SPACE_JOINER = Joiner.on(" ");
        HEX = BaseEncoding.base16().lowerCase();
        UTC = TimeZone.getTimeZone("UTC");
        bitMask = new int[]{1, 2, 4, 8, 16, 32, 64, 128};
        isAndroid = -1;
    }

    private static class Pair implements Comparable<Utils.Pair> {
        int item;
        int count;

        public Pair(int item, int count) {
            this.count = count;
            this.item = item;
        }

        public int compareTo(Utils.Pair o) {
            return -Ints.compare(this.count, o.count);
        }
    }
}
