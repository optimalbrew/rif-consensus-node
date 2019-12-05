package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.lang.ref.SoftReference;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper modeling a value stored in a {@link UniNode}. A {@link UniNode}
 * having a value can store:
 *
 *  - The value itself + value hash + value length in bytes. This will happen if the value
 *    length in bytes is under a certain threshold.
 *  - No value, but the hash and a key. This will happen if the value length in bytes
 *    is larger than the threshold. In this case the hash will act as a key that can
 *    be used to hit a ley-value store and retrieve the actual value.
 *
 * @author ppedemon
 */
public final class ValueWrapper {

    private static int MAX_SHORT_LEN = 32;

    public static ValueWrapper fromValue(final BytesValue value) {
        Preconditions.checkNotNull(value, "Value can't be null");
        return new ValueWrapper(value, Hash.keccak256(value), UInt24.fromInt(value.size()));
    }

    public static ValueWrapper fromHash(final Bytes32 hash, final UInt24 length) {
        Preconditions.checkNotNull(hash, "Hash can't be null");
        Preconditions.checkNotNull(length, "Length can't be null");
        return new ValueWrapper(null, hash, length);
    }

    private SoftReference<BytesValue> value;
    private Bytes32 hash;
    private UInt24 length;

    private ValueWrapper(final BytesValue value, final Bytes32 hash, final UInt24 length) {
        this.value = Objects.isNull(value)? null : new SoftReference<>(value);
        this.hash = hash;
        this.length = length;
    }

    BytesValue solveValue(final MerkleStorage storage) {
        if (value != null) {
            BytesValue v = value.get();
            if (v != null) {
                return v;
            }
        }
        BytesValue v = storage.get(hash).orElseThrow(() -> new NoSuchElementException("No value for hash: " + hash));
        value = new SoftReference<>(v);
        return v;
    }

    Bytes32 getHash() {
        return hash;
    }

    UInt24 getLength() {
        return length;
    }

    boolean wrappedValueIs(final BytesValue value) {
        return Hash.keccak256(value).equals(hash);
    }

    boolean isLong() {
        return length.toInt() <= MAX_SHORT_LEN;
    }

    @Override
    public String toString() {
        return String.format("(%s, hash=%s, len=%s)", value, hash, length);
    }
}
