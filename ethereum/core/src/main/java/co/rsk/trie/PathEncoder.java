/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.trie;

import org.hyperledger.besu.util.bytes.BytesValue;

import javax.annotation.Nonnull;

/**
 * Created by martin.medina on 5/04/17.
 */
public class PathEncoder {
    private PathEncoder() { }

    @Nonnull
    public static BytesValue encode(BytesValue path) {
        if (path == null) {
            throw new IllegalArgumentException("path");
        }

        return encodeBinaryPath(path);
    }

    @Nonnull
    public static BytesValue decode(BytesValue encoded, int length) {
        if (encoded == null) {
            throw new IllegalArgumentException("encoded");
        }

        return decodeBinaryPath(encoded, length);
    }

    @Nonnull
    // First bit is MOST SIGNIFICANT
    private static BytesValue encodeBinaryPath(BytesValue path) {
        int lpath = path.size();
        int lencoded = calculateEncodedLength(lpath);

        byte[] encoded = new byte[lencoded];
        int nbyte = 0;

        for (int k = 0; k < lpath; k++) {
            int offset = k % 8;
            if (k > 0 && offset == 0) {
                nbyte++;
            }

            if (path.get(k) == 0) {
                continue;
            }

            encoded[nbyte] |= 0x80 >> offset;
        }

        return BytesValue.wrap(encoded);
    }

    @Nonnull
    // length is the length in bits. For example ({1},8) is fine
    // First bit is MOST SIGNIFICANT
    private static BytesValue decodeBinaryPath(BytesValue encoded, int bitlength) {
        byte[] path = new byte[bitlength];

        for (int k = 0; k < bitlength; k++) {
            int nbyte = k / 8;
            int offset = k % 8;

            if (((encoded.get(nbyte) >> (7 - offset)) & 0x01) != 0) {
                path[k] = 1;
            }
        }

        return BytesValue.wrap(path);
    }

    public static int calculateEncodedLength(int keyLength) {
        return keyLength / 8 + (keyLength % 8 == 0 ? 0 : 1);
    }
}
