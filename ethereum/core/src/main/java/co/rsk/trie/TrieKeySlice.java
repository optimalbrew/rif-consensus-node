/*
 * This file is part of RskJ
 * Copyright (C) 2019 RSK Labs Ltd.
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
import org.hyperledger.besu.util.bytes.MutableBytesValue;

//import java.util.Arrays;

/**
 * An immutable slice of a trie key.
 * Sub-slices share array references, so external sources are copied and the internal array is not exposed.
 */
public class TrieKeySlice {
    private final BytesValue expandedKey;
    private final int offset;
    private final int limit;

    private TrieKeySlice(BytesValue expandedKey, int offset, int limit) {
        this.expandedKey = expandedKey;
        this.offset = offset;
        this.limit = limit;
    }

    public int length() {
        return limit - offset;
    }

    public byte get(int i) {
        return expandedKey.get(offset + i);
    }

    public BytesValue encode() {
        // TODO(mc) avoid copying by passing the indices to PathEncoder.encode
        // TODO: (RL): Besu's slice method does exactly that so it solves above's todo
        //expandedkey.slice doesn't copy, it's a reference
        return PathEncoder.encode(expandedKey.slice(offset, limit));
    }

    public TrieKeySlice slice(int from, int to) {
        if (from < 0) {
            throw new IllegalArgumentException("The start position must not be lower than 0");
        }

        if (from > to) {
            throw new IllegalArgumentException("The start position must not be greater than the end position");
        }

        int newOffset = offset + from;
        if (newOffset > limit) {
            throw new IllegalArgumentException("The start position must not exceed the key length");
        }

        int newLimit = offset + to;
        if (newLimit > limit) {
            throw new IllegalArgumentException("The end position must not exceed the key length");
        }

        return new TrieKeySlice(expandedKey, newOffset, newLimit);
    }

    public TrieKeySlice commonPath(TrieKeySlice other) {
        int maxCommonLengthPossible = Math.min(length(), other.length());
        for (int i = 0; i < maxCommonLengthPossible; i++) {
            if (get(i) != other.get(i)) {
                return slice(0, i);
            }
        }

        return slice(0, maxCommonLengthPossible);
    }

    /**
     * Rebuild a shared path as [...this, implicitByte, ...childSharedPath]
     */
    public TrieKeySlice rebuildSharedPath(byte implicitByte, TrieKeySlice childSharedPath) {
        int length = length();
        int childSharedPathLength = childSharedPath.length(); //already removes the offset
        int newLength = length + 1 + childSharedPathLength;
        MutableBytesValue newExpandedKey = MutableBytesValue.wrap(expandedKey.copyRange(offset, offset + newLength).getArrayUnsafe());
        newExpandedKey.set(length, implicitByte);

        childSharedPath.expandedKey.copyTo(newExpandedKey.getArrayUnsafe(), childSharedPath.offset, length+1, childSharedPathLength);

        //byte[] newExpandedKey = Arrays.copyOfRange(expandedKey, offset, offset + newLength);
        //newExpandedKey[length] = implicitByte;
          /*System.arraycopy(
                childSharedPath.expandedKey, childSharedPath.offset,
                newExpandedKey, length + 1, childSharedPathLength);*/


        return new TrieKeySlice(newExpandedKey, 0, newExpandedKey.size());
    }

    public TrieKeySlice leftPad(int paddingLength) {
        if (paddingLength == 0) {
            return this;
        }
        int currentLength = length();
        byte[] paddedExpandedKey = new byte[currentLength + paddingLength];

        expandedKey.copyTo(paddedExpandedKey, offset, paddingLength, currentLength );
        //System.arraycopy(expandedKey, offset, paddedExpandedKey, paddingLength, currentLength);
        return new TrieKeySlice(BytesValue.wrap(paddedExpandedKey), 0, paddedExpandedKey.length);
    }

    public static TrieKeySlice fromKey(BytesValue key) {
        BytesValue expandedKey = PathEncoder.decode(key, key.size() * 8);
        return new TrieKeySlice(expandedKey, 0, expandedKey.size());
    }

    public static TrieKeySlice fromEncoded(BytesValue src, int offset, int keyLength, int encodedLength) {
        // TODO(mc) avoid copying by passing the indices to PathEncoder.decode
        BytesValue encodedKey = src.slice(offset, encodedLength);
        BytesValue expandedKey = PathEncoder.decode(encodedKey, keyLength);

        //byte[] encodedKey = Arrays.copyOfRange(src, offset, offset + encodedLength);
        //byte[] expandedKey = PathEncoder.decodeOfRange(src, keyLength);
        return new TrieKeySlice(expandedKey, 0, expandedKey.size());
    }

    public static TrieKeySlice empty() {
        return new TrieKeySlice(BytesValue.EMPTY, 0, 0);
    }
}
