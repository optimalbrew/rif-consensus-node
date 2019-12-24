/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Preconditions;

/**
 * Wrapper modeling a value stored in a {@link UniNode}. A {@link UniNode} can store:
 *
 * <ul>
 *   <li>No value at all.
 *   <li>The value itself + value hash + value length in bytes. This will happen if the value length
 *       in bytes is under a certain threshold.
 *   <li>No value, but the hash and a key. This will happen if the value length in bytes is larger
 *       than the threshold. In this case the hash will act as a key that can be used to hit a
 *       ley-value store and retrieve the actual value.
 * </ul>
 *
 * @author ppedemon
 */
public final class ValueWrapper {

  /** Sole instance of an empty value wrapper. */
  public static ValueWrapper EMPTY = empty();

  /** Maximum length in bytes of short (inlined) value. */
  private static int MAX_SHORT_LEN = 32;

  private BytesValue value;
  private Bytes32 hash;
  private UInt24 length;

  private ValueWrapper(final BytesValue value, final Bytes32 hash, final UInt24 length) {
    this.value = value;
    this.hash = hash;
    this.length = length;
  }

  /**
   * Construct empty value wrapper.
   *
   * @return empty value wrapper
   */
  private static ValueWrapper empty() {
    return new ValueWrapper(null, null, null);
  }

  /**
   * Construct a value wrapper from the given value.
   *
   * @param value value to wrap
   * @return instance wrapping the given value
   */
  public static ValueWrapper fromValue(final BytesValue value) {
    Preconditions.checkNotNull(value, "Value can't be null");
    return new ValueWrapper(value, Hash.keccak256(value), UInt24.fromInt(value.size()));
  }

  /**
   * Construct a value wrapper for a long value given as a hash and length.
   *
   * @param hash long value hash
   * @param length long value length
   * @return instance wrapping the given long value.
   */
  public static ValueWrapper fromHash(final Bytes32 hash, final UInt24 length) {
    Preconditions.checkNotNull(hash, "Hash can't be null");
    Preconditions.checkNotNull(length, "Length can't be null");
    return new ValueWrapper(null, hash, length);
  }

  /**
   * Decode a value wrapper instance from the given buffer.
   *
   * @param buffer buffer to decode from
   * @param isLong whether the value to decode is long
   * @return decoded value wrapper instance
   */
  static ValueWrapper decodeFrom(final ByteBuffer buffer, final boolean isLong) {
    if (isLong) {
      byte[] valueHashBytes = new byte[Bytes32.SIZE];
      buffer.get(valueHashBytes);
      Bytes32 hash = Bytes32.wrap(valueHashBytes);
      byte[] valueLengthBytes = new byte[UInt24.BYTES];
      buffer.get(valueLengthBytes);
      UInt24 length = UInt24.fromBytes(valueLengthBytes);
      return fromHash(hash, length);
    }

    int remaining = buffer.remaining();
    if (remaining != 0) {
      byte[] value = new byte[remaining];
      buffer.get(value);
      return fromValue(BytesValue.wrap(value));
    }

    return empty();
  }

  /**
   * Solve the wrapped value by looking it up in storage if necessary.
   *
   * @param loader data loader to use if the value isn't cached and it's long
   * @return optional wrapped value if present, or empty optional if this wrapper is empty
   * @throws NoSuchElementException if value is long and can't be solved from the given storage
   */
  Optional<BytesValue> solveValue(final DataLoader loader) {
    if (isEmpty()) {
      return Optional.empty();
    }

    if (value != null) {
      return Optional.of(value);
    }

    BytesValue v =
        loader.load(hash).orElseThrow(() -> new NoSuchElementException("Unsolvable hash: " + hash));

    if (!Hash.keccak256(v).equals(hash)) {
      throw new IllegalStateException("Solved value hash differs from wrapped hash");
    }
    if (v.size() != length.toInt()) {
      throw new IllegalStateException("Solved value length differs from wrapped length");
    }

    value = v;
    return Optional.of(value);
  }

  /**
   * Get optional hash for wrapped value.
   *
   * @return optional with hash if wrapper isn't empty, empty optional otherwise
   */
  Optional<Bytes32> getHash() {
    return Optional.ofNullable(hash);
  }

  /**
   * Get optional value length for wrapped value.
   *
   * @return optional with value length in bytes if wrapper isn't empty, empty optional otherwise
   */
  Optional<UInt24> getLength() {
    return Optional.ofNullable(length);
  }

  /**
   * Answer whether this wrapper is empty
   *
   * @return whether this wrapper is empty
   */
  boolean isEmpty() {
    return Objects.isNull(hash);
  }

  /**
   * Equality test: answer true if wrapped value hash is equal the given one's hash.
   *
   * @param value value to test for equality using hash
   * @return whether the given value hash is equal to the wrapped value hash
   */
  boolean wrappedValueIs(final BytesValue value) {
    return !isEmpty() && Hash.keccak256(value).equals(hash);
  }

  /**
   * Is the wrapped value long?
   *
   * @return whether the wrapped value is long
   */
  boolean isLong() {
    return !isEmpty() && length.toInt() > MAX_SHORT_LEN;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "[empty]";
    }

    return String.format("(%s, hash=%s, len=%s)", value, hash, length);
  }

  /**
   * Encode this instance to the given buffer.
   *
   * @param buffer buffer to encode to
   */
  void encodeTo(final ByteBuffer buffer) {
    if (isEmpty()) {
      return;
    }

    if (isLong()) {
      buffer.put(hash.getArrayUnsafe());
      buffer.put(length.toByteArray());
    } else {
      buffer.put(value.getArrayUnsafe());
    }
  }
}
