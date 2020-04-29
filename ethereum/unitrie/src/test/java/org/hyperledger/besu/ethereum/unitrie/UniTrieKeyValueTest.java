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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.unitrie.ByteTestUtils.bytes;

import java.nio.charset.StandardCharsets;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;

public class UniTrieKeyValueTest {

  private static UniNode NO_RESULT = NullUniNode.instance();

  private final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
  private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory();
  private final DataLoader loader = storage::get;

  @Test
  public void putWithEmptyKey() {
    UniNode trie = NullUniNode.instance();
    trie = trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.EMPTY);

    assertThat(trie.accept(new GetVisitor(), Bytes.EMPTY).getValue(loader)).hasValue(bytes(1));
  }

  @Test
  public void putSingleValue() {
    UniNode trie = NullUniNode.instance();
    trie = trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0)).getValue(loader))
        .hasValue(bytes(1));
  }

  @Test
  public void putSingleValueTwice() {
    UniNode trie = NullUniNode.instance();
    UniNode trie0 = trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0));
    UniNode trie1 =
        trie0.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0));

    assertThat(trie0.accept(new GetVisitor(), Bytes.of(0, 0)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie1.accept(new GetVisitor(), Bytes.of(0, 0)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie0).isSameAs(trie1);
  }

  @Test
  public void putSingleAndReplace() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0)).getValue(loader))
        .hasValue(bytes(2));
  }

  @Test
  public void splitLeftWithNoLeaf() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0)).getValue(loader))
        .hasValue(bytes(2));
  }

  @Test
  public void splitRightWithNoLeaf() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0)).getValue(loader))
        .hasValue(bytes(2));
  }

  @Test
  public void splitLeftWithNewLeaf() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 1, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 0)).getValue(loader))
        .hasValue(bytes(2));
  }

  @Test
  public void splitRightWithNewLeaf() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(2));
  }

  @Test
  public void recurse() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(2));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0)).getValue(loader))
        .hasValue(bytes(3));
  }

  @Test
  public void recurseShort() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(2));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1)).getValue(loader))
        .hasValue(bytes(3));
  }

  @Test
  public void recurseAndReplaceRoot() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new PutVisitor(bytes(4), nodeFactory), Bytes.of(0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(2));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0)).getValue(loader))
        .hasValue(bytes(3));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0)).getValue(loader))
        .hasValue(bytes(4));
  }

  @Test
  public void recurseAndPutEmpty() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new PutVisitor(bytes(4), nodeFactory), Bytes.EMPTY);

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0)).getValue(loader))
        .hasValue(bytes(3));
    assertThat(trie.accept(new GetVisitor(), Bytes.EMPTY).getValue(loader))
        .hasValue(bytes(4));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(2));
  }

  @Test
  public void notFound() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new PutVisitor(bytes(4), nodeFactory), Bytes.EMPTY);

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1))).isEqualTo(NO_RESULT);
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 0))).isEqualTo(NO_RESULT);
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0, 1, 1)))
        .isEqualTo(NO_RESULT);
  }

  @Test
  public void putAWholeLotOfStuff() {
    UniNode trie = NullUniNode.instance();
    for (int i = 0; i < 100; i++) {
      Bytes value = Bytes.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
      Bytes path = PathEncoding.decodePath(value, value.size() * 8);
      trie = trie.accept(new PutVisitor(value.extractArray(), nodeFactory), path);
    }

    for (int i = 0; i < 100; i++) {
      Bytes value = Bytes.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
      Bytes path = PathEncoding.decodePath(value, value.size() * 8);
      assertThat(trie.accept(new GetVisitor(), path).getValue(loader))
          .hasValue(value.extractArray());
    }
  }

  @Test
  public void removeNonExistent() {
    UniNode trie = NullUniNode.instance();
    UniNode trie0 =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0));
    UniNode trie1 = trie0.accept(new RemoveVisitor(nodeFactory), Bytes.of(0, 1, 1, 1, 1));

    assertThat(trie0).isSameAs(trie1);
  }

  @Test
  public void removeRoot() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 0))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0))
            .accept(new RemoveVisitor(nodeFactory), Bytes.of(0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0))).isEqualTo(NO_RESULT);
  }

  @Test
  public void removeWithCoalescing() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new RemoveVisitor(nodeFactory), Bytes.of(0, 0, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0)).getValue(loader))
        .hasValue(bytes(3));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0))).isSameAs(NO_RESULT);
  }

  @Test
  public void removeWithoutCoalescing() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new RemoveVisitor(nodeFactory), Bytes.of(0, 0, 0, 1, 0));

    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .hasValue(bytes(1));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .hasValue(bytes(2));
    assertThat(trie.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0))).isSameAs(NO_RESULT);
  }

  @Test
  public void removeEvenKeys() {
    UniNode trie = NullUniNode.instance();
    for (int i = 0; i < 100; i++) {
      Bytes value = Bytes.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
      Bytes path = PathEncoding.decodePath(value, value.size() * 8);
      trie = trie.accept(new PutVisitor(value.extractArray(), nodeFactory), path);
    }

    for (int i = 0; i < 100; i++) {
      if (i % 2 == 1) {
        Bytes value = Bytes.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
        Bytes path = PathEncoding.decodePath(value, value.size() * 8);
        trie = trie.accept(new RemoveVisitor(nodeFactory), path);
      }
    }

    for (int i = 0; i < 100; i++) {
      Bytes value = Bytes.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
      Bytes path = PathEncoding.decodePath(value, value.size() * 8);
      if (i % 2 == 0) {
        assertThat(trie.accept(new GetVisitor(), path).getValue(loader))
            .hasValue(value.extractArray());
      } else {
        assertThat(trie.accept(new GetVisitor(), path)).isSameAs(NO_RESULT);
      }
    }
  }

  @Test
  public void removeRecursiveNotPresent() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new PutVisitor(bytes(4), nodeFactory), Bytes.EMPTY);
    assertThat(trie.accept(new RemoveVisitor(true, nodeFactory), Bytes.of(1, 1, 1)))
        .isSameAs(trie);
  }

  @Test
  public void removeRootRecursively_leavesEmptyTrie() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new PutVisitor(bytes(4), nodeFactory), Bytes.EMPTY);
    assertThat(trie.accept(new RemoveVisitor(true, nodeFactory), Bytes.EMPTY))
        .isSameAs(NullUniNode.instance());
  }

  @Test
  public void removeNodeRecursively() {
    UniNode trie = NullUniNode.instance();
    trie =
        trie.accept(new PutVisitor(bytes(1), nodeFactory), Bytes.of(0, 1, 1))
            .accept(new PutVisitor(bytes(2), nodeFactory), Bytes.of(0, 0, 0))
            .accept(new PutVisitor(bytes(3), nodeFactory), Bytes.of(0, 0, 0, 1, 0))
            .accept(new PutVisitor(bytes(4), nodeFactory), Bytes.EMPTY);

    UniNode stripped = trie.accept(new RemoveVisitor(true, nodeFactory), Bytes.of(0, 0, 0));

    assertThat(stripped.accept(new GetVisitor(), Bytes.of(0, 1, 1)).getValue(loader))
        .contains(bytes(1));
    assertThat(stripped.accept(new GetVisitor(), Bytes.EMPTY).getValue(loader))
        .contains(bytes(4));
    assertThat(stripped.accept(new GetVisitor(), Bytes.of(0, 0, 0)).getValue(loader))
        .isEmpty();
    assertThat(stripped.accept(new GetVisitor(), Bytes.of(0, 0, 0, 1, 0)).getValue(loader))
        .isEmpty();
  }
}
