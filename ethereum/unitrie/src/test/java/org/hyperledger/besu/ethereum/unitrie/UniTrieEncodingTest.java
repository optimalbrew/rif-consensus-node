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
 *
 */
package org.hyperledger.besu.ethereum.unitrie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.unitrie.ByteTestUtils.bytes;

import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

public class UniTrieEncodingTest {

  private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory();

  @Test
  public void emptyTree_givesNullEncoding() {
    assertThat(NullUniNode.instance().getEncoding())
        .isEqualTo(UniTrie.NULL_UNINODE_ENCODING.extractArray());
  }

  @Test
  public void emptyPath_encodesCorrectly() {
    byte[] value = bytes(1, 2, 3);
    UniNode trie = nodeFactory.createLeaf(bytes(), ValueWrapper.fromValue(value));
    assertThat(BytesValue.of(trie.getEncoding()))
        .isEqualTo(BytesValue.of(0x40).concat(BytesValue.of(value)));
  }

  @Test
  public void leaf_encodesCorrectly() {
    byte[] path = bytes(1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0);
    byte[] value = bytes(1, 2, 3);
    UniNode trie = nodeFactory.createLeaf(path, ValueWrapper.fromValue(value));
    assertThat(BytesValue.of(trie.getEncoding()))
        .isEqualTo(BytesValue.of(0x50, 0x0a, 0xaf, 0x40).concat(BytesValue.of(value)));
  }

  @Test
  public void embeddedChildren_encodesCorrectly() {
    byte[] valueTop = bytes(9);
    byte[] valueLeft = bytes(1, 2, 3, 4);
    byte[] valueRight = bytes(5, 6, 7, 8);
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(valueLeft, nodeFactory), BytesValue.of(1, 1, 0, 0))
            .accept(new PutVisitor(valueRight, nodeFactory), BytesValue.of(1, 1, 1, 1))
            .accept(new PutVisitor(valueTop, nodeFactory), BytesValue.of(1, 1));
    assertThat(BytesValue.of(trie.getEncoding()))
        .isEqualTo(BytesValue.fromHexString("0x5f01c00750000001020304075000800506070809"));
  }

  @Test
  public void longChildren_encodesCorrectly() {
    Bytes32 hash1 =
        Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555555");
    Bytes32 hash2 =
        Bytes32.fromHexString("0x5655555555555555555555555555555555555555555555555555555555555556");

    BytesValue path1 = PathEncoding.decodePath(hash1, 256);
    BytesValue path2 = PathEncoding.decodePath(hash2, 256);

    byte[] value = makeValue(1000);
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(value, nodeFactory), path1)
            .accept(new PutVisitor(value, nodeFactory), path2)
            .accept(new PutVisitor(bytes(9), nodeFactory), BytesValue.of(0, 1, 0, 1, 0, 1));

    byte[] enc = trie.getEncoding();

    // No long val, has path, has left and right children, none embedded
    assertThat(enc[0]).isEqualTo((byte) 0b01011100);

    // Path length is 6 (encoded as 6-1 = 5)
    assertThat(enc[1]).isEqualTo((byte) 5);

    // Root node encoded path must be common part of {0x55, 0x56} = 0b010101
    BytesValue b = BytesValue.of(enc);
    assertThat(PathEncoding.decodePath(b.slice(2, 1), b.get(1) + 1))
        .isEqualTo(BytesValue.of(0, 1, 0, 1, 0, 1));

    // Value must be 9, at the end of the encoding
    assertThat(enc[enc.length - 1]).isEqualTo((byte) 9);
  }

  private static byte[] makeValue(final int length) {
    byte[] value = new byte[length];

    for (int k = 0; k < length; k++) {
      value[k] = (byte) ((k + 1) % 256);
    }

    return value;
  }
}
