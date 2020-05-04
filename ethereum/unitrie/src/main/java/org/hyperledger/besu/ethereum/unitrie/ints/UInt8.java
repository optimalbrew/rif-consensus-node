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
package org.hyperledger.besu.ethereum.unitrie.ints;

import org.apache.tuweni.bytes.Bytes;

/** Type representing an 8 bits (single byte) unsigned value. */
public final class UInt8 implements Comparable<UInt8> {

  public static final int BITS = 8;
  public static final int BYTES = BITS / Byte.SIZE;

  public static final UInt8 ZERO = new UInt8(0);

  private static final int MAX_INTEGER_VALUE = 0xff;
  public static final UInt8 MAX_VALUE = new UInt8(MAX_INTEGER_VALUE);

  private final int intValue;

  public static UInt8 fromBytes(final byte[] bytes) {
    return fromBytes(bytes, 0);
  }

  public static UInt8 fromBytes(final byte[] bytes, final int offset) {
    int intValue = bytes[offset] & 0xFF;
    return new UInt8(intValue);
  }

  public static UInt8 fromBytesValue(final Bytes value) {
    return fromBytesValue(value, 0);
  }

  public static UInt8 fromBytesValue(final Bytes value, final int offset) {
    int intValue = value.get(offset) & 0xff;
    return new UInt8(intValue);
  }

  public UInt8(final int intValue) {
    if (intValue < 0 || intValue > MAX_INTEGER_VALUE) {
      throw new IllegalArgumentException("The supplied value doesn't fit in a UInt8 instance");
    }
    this.intValue = intValue;
  }

  public byte asByte() {
    return (byte) intValue;
  }

  public byte[] toByteArray() {
    byte[] bytes = new byte[BYTES];
    bytes[0] = asByte();
    return bytes;
  }

  public int intValue() {
    return intValue;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UInt8 uint8 = (UInt8) o;
    return intValue == uint8.intValue;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(intValue);
  }

  @Override
  public int compareTo(final UInt8 other) {
    return Integer.compare(intValue, other.intValue);
  }

  @Override
  public String toString() {
    return Integer.toString(intValue);
  }
}
