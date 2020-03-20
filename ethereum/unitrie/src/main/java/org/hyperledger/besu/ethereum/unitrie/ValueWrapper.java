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

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Wrapper modeling a value stored in a {@link UniNode}. A {@link UniNode} can store:
 *
 * <ul>
 *   <li>No value at all.
 *   <li>The value itself + value hash + value length in bytes. This will happen if the value length
 *       in bytes is under a certain threshold.
 *   <li>No value, but the hash and length. This will happen if the value length in bytes is larger
 *       than the threshold. In this case the hash will act as a key that can be used to hit a
 *       key-value store and retrieve the actual value.
 * </ul>
 *
 * @author ppedemon
 */
public final class ValueWrapper {

  /** Sole instance of an empty value wrapper. */
  public static ValueWrapper EMPTY = empty();

  /** Maximum length in bytes of short (inlined) value. */
  private static int MAX_SHORT_LEN = 32;

  private byte[] value;
  private byte[] hash;
  private Integer length;

  private ValueWrapper(final byte[] value, final byte[] hash, final int length) {
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
    return new ValueWrapper(null, null, -1);
  }

  /**
   * Construct a value wrapper from the given value.
   *
   * @param value value to wrap
   * @return instance wrapping the given value
   */
  public static ValueWrapper fromValue(final byte[] value) {
    Preconditions.checkNotNull(value, "Value can't be null");
    Bytes32 hash = Hash.keccak256(BytesValue.of(value));
    return new ValueWrapper(value, hash.extractArray(), value.length);
  }

  /**
   * Construct a value wrapper for a long value given as a hash and length.
   *
   * @param hash long value hash
   * @param length long value length
   * @return instance wrapping the given long value.
   */
  private static ValueWrapper fromHash(final byte[] hash, final int length) {
    Preconditions.checkNotNull(hash, "Hash can't be null");
    Preconditions.checkArgument(length >= 0, "length must be strictly positive");
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
      byte[] hash = new byte[Bytes32.SIZE];
      buffer.get(hash);
      byte[] lengthBytes = new byte[UInt24.BYTES];
      buffer.get(lengthBytes);
      int length = UInt24.fromBytes(lengthBytes).toInt();
      return fromHash(hash, length);
    }

    int remaining = buffer.remaining();
    if (remaining != 0) {
      byte[] value = new byte[remaining];
      buffer.get(value);
      return fromValue(value);
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
  Optional<byte[]> solveValue(final DataLoader loader) {
    if (isEmpty()) {
      return Optional.empty();
    }

    if (value != null) {
      return Optional.of(value);
    }

    Bytes32 h = Bytes32.wrap(hash);
    BytesValue v =
        loader.load(h).orElseThrow(() -> new NoSuchElementException("Unsolvable hash: " + h));

    if (!Hash.keccak256(v).equals(h)) {
      throw new IllegalStateException("Solved value hash differs from wrapped hash");
    }
    if (v.size() != length) {
      throw new IllegalStateException("Solved value length differs from wrapped length");
    }

    value = v.extractArray();
    return Optional.of(value);
  }

  /**
   * Get optional hash for wrapped value.
   *
   * @return optional with hash if wrapper isn't empty, empty optional otherwise
   */
  Optional<byte[]> getHash() {
    return Optional.ofNullable(hash);
  }

  /**
   * Get optional value length for wrapped value.
   *
   * @return optional with value length in bytes if wrapper isn't empty, empty optional otherwise
   */
  Optional<Integer> getLength() {
    return length == -1? Optional.empty() : Optional.of(length);
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
   * Equality test: answer true if wrapped value is equal the given value.
   *
   * @param value value to test for equality
   * @return whether the given value is equal to the wrapped one, always false if wrapper is empty
   */
  boolean wrappedValueIs(final byte[] value) {
    // Compute value's hash as the last resource to determine equality
    if (isEmpty() || value.length != length) {
      return false;
    }
    return (this.value != null && Arrays.equals(value, this.value))
        || Hash.keccak256(BytesValue.of(value)).equals(Bytes32.wrap(hash));
  }

  /**
   * Is the wrapped value long?
   *
   * @return whether the wrapped value is long
   */
  public boolean isLong() {
    return !isEmpty() && length > MAX_SHORT_LEN;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "[empty]";
    }

    return String.format(
        "(%s, hash=%s, len=%d)", BytesValue.wrap(value), Bytes32.wrap(hash), length);
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
      buffer.put(hash);
      buffer.put(UInt24.fromInt(length).toByteArray());
    } else {
      buffer.put(value);
    }
  }
}
