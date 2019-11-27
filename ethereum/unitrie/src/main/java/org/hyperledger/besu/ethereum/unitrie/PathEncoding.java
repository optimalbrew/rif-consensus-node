package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.bytes.MutableBytesValue;

/**
 * Encode or decode shared paths.
 *
 * @author ppedemon
 */
public final class PathEncoding {

    /**
     * Decode the given encoded path, turning it into a sequence of binary digits.
     * For example, {0xA1} becomes {1, 0, 1, 0, 0, 0, 0, 1}.
     *
     * @param encodedPath          path to decode
     * @param decodedLengthInBits  number of intended binary digits in decoded path
     * @return  decoded path, given by a sequence of binary digits of length {@code decodedLengthBits}
     */
    public static BytesValue decodePath(final BytesValue encodedPath, final int decodedLengthInBits) {
        Preconditions.checkNotNull(encodedPath, "Encoded path is null");

        MutableBytesValue decoded = MutableBytesValue.create(decodedLengthInBits);

        for (int i = 0; i < decodedLengthInBits; i++) {
            int index = i / 8;
            int offset = i % 8;
            decoded.set(i, (byte) (((encodedPath.get(index) >>> (7 - offset)) & 0x01) == 1? 1: 0));
        }

        return decoded;
    }

    /**
     * Encode the given path, turning it into a sequence of bytes.
     * For example, the path {1, 0, 1, 0, 0, 0, 0, 1, 0, 1} becomes {0xA1, 0x40}.
     *
     * @param path  path as a sequence of binary digits
     * @return  encoded path
     */
    public static BytesValue encodePath(final BytesValue path) {
        Preconditions.checkNotNull(path, "Path to encode is null");

        int length = (path.size() + 7) / 8;
        MutableBytesValue encoded = MutableBytesValue.create(length);

        int index = 0;
        for (int i = 0; i < path.size(); i++) {
            int offset = i % 8;
            if (i > 0 && offset == 0) {
                ++index;
            }
            if (path.get(i) == 0) {
                continue;
            }
            encoded.set(index, (byte) (encoded.get(index) | (0x80 >> offset)));
        }

        return encoded;
    }
}
