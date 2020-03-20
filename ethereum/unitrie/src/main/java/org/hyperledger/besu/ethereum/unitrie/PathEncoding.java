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

import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.bytes.MutableBytesValue;

import com.google.common.base.Preconditions;

/**
 * Encode or decode shared paths.
 *
 * @author ppedemon
 */
public final class PathEncoding {

  /**
   * Compute the length in bytes for a path with the given number of bits.
   *
   * @param pathLengthInBits path length in bytes
   * @return number of bytes required to encode a path with the given length in bits
   */
  static int encodedPathLength(final int pathLengthInBits) {
    return (pathLengthInBits + 7) / 8;
  }

  /**
   * Decode the given encoded path, turning it into a sequence of binary digits. For example, {0xA1}
   * becomes {1, 0, 1, 0, 0, 0, 0, 1}.
   *
   * @param encodedPath path to decode
   * @param decodedLengthInBits number of intended binary digits in decoded path
   * @return decoded path, given by a sequence of binary digits of length {@code decodedLengthBits}
   */
  public static BytesValue decodePath(final BytesValue encodedPath, final int decodedLengthInBits) {
    Preconditions.checkNotNull(encodedPath, "Encoded path is null");

    MutableBytesValue decoded = MutableBytesValue.create(decodedLengthInBits);

    for (int i = 0; i < decodedLengthInBits; i++) {
      int index = i / 8;
      int offset = i % 8;
      decoded.set(i, (byte) (((encodedPath.get(index) >>> (7 - offset)) & 0x01) == 1 ? 1 : 0));
    }

    return decoded;
  }

  /**
   * Encode the given path, turning it into a sequence of bytes. For example, the path {1, 0, 1, 0,
   * 0, 0, 0, 1, 0, 1} becomes {0xA1, 0x40}.
   *
   * @param path path as a sequence of binary digits
   * @return encoded path
   */
  public static BytesValue encodePath(final BytesValue path) {
    Preconditions.checkNotNull(path, "Path to encode is null");

    int length = encodedPathLength(path.size());
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
