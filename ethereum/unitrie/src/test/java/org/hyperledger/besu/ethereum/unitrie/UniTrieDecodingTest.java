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

import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;

public class UniTrieDecodingTest {
  private final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
  private final StoredUniNodeFactory nodeFactory = new StoredUniNodeFactory(storage::get);

  @Test
  public void emptyPath_loadsCorrectly() {
    UniNode trie = nodeFactory.createLeaf(bytes(), ValueWrapper.fromValue(bytes(1, 2, 3)));
    byte[] enc = trie.getEncoding();
    UniNode decoded = nodeFactory.decode(enc);
    assertThat(trie.getHash()).isEqualTo(decoded.getHash());
  }

  @Test
  public void leaf_loadsCorrectly() {
    byte[] path = bytes(1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0);
    byte[] value = bytes(1, 2, 3);
    UniNode trie = nodeFactory.createLeaf(path, ValueWrapper.fromValue(value));
    byte[] enc = trie.getEncoding();
    UniNode decoded = nodeFactory.decode(enc);
    assertThat(trie.getHash()).isEqualTo(decoded.getHash());
  }

  @Test
  public void embeddedLeftChild_loadsCorrectly() {
    byte[] valueTop = bytes(9);
    byte[] valueLeft = bytes(1, 2, 3, 4);
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(valueLeft, nodeFactory), Bytes.of(1, 1, 0, 0))
            .accept(new PutVisitor(valueTop, nodeFactory), Bytes.of(1, 1));
    byte[] enc = trie.getEncoding();
    UniNode decoded = nodeFactory.decode(enc);
    assertThat(trie.getHash()).isEqualTo(decoded.getHash());
  }

  @Test
  public void embeddedRightChild_loadsCorrectly() {
    byte[] valueTop = bytes(9);
    byte[] valueRight = bytes(5, 6, 7, 8);
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(valueRight, nodeFactory), Bytes.of(1, 1, 1, 1))
            .accept(new PutVisitor(valueTop, nodeFactory), Bytes.of(1, 1));

    byte[] enc = trie.getEncoding();
    UniNode decoded = nodeFactory.decode(enc);

    assertThat(trie.getHash()).isEqualTo(decoded.getHash());
  }

  @Test
  public void embeddedChildren_loadsCorrectly() {
    byte[] valueTop = bytes(9);
    byte[] valueLeft = bytes(1, 2, 3, 4);
    byte[] valueRight = bytes(5, 6, 7, 8);
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(valueLeft, nodeFactory), Bytes.of(1, 1, 0, 0))
            .accept(new PutVisitor(valueRight, nodeFactory), Bytes.of(1, 1, 1, 1))
            .accept(new PutVisitor(valueTop, nodeFactory), Bytes.of(1, 1));
    byte[] enc = trie.getEncoding();
    UniNode decoded = nodeFactory.decode(enc);
    assertThat(trie.getHash()).isEqualTo(decoded.getHash());
  }
}
