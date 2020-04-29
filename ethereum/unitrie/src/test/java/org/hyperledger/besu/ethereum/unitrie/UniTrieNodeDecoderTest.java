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

import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import org.junit.Test;

import static org.hyperledger.besu.ethereum.unitrie.ByteTestUtils.bytes;

public class UniTrieNodeDecoderTest {

  @Test
  public void decodeUniNodes() {
    final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());

    // Build a small trie
    UniTrie<Bytes, Bytes> trie =
        new StoredUniTrie<>(storage::get, Function.identity(), Function.identity());

    trie.put(Bytes.of(0, 0), Bytes.of(1));
    trie.put(Bytes.of(0, 1), Bytes.of(2));
    trie.put(Bytes.of(1, 0), Bytes.of(3));

    // Create large leaf node that will not be inlined
    trie.put(
        Bytes.fromHexString("0x" + Strings.repeat("01", 32)),
        Bytes.fromHexString("0x" + Strings.repeat("bad", 50)));

    // Save nodes to storage
    trie.commit(storage::put, storage::put);

    // Get and flatten root node
    final Bytes encodedRoot =
        storage
            .get(trie.getRootHash())
            .orElseGet(
                () -> {
                  throw new IllegalStateException("Unable to decode trie root");
                });

    UniTrieNodeDecoder decoder = new UniTrieNodeDecoder(storage::get);

    final List<UniNode> nodes = decoder.decodeNodes(encodedRoot);
    assertThat(nodes.size()).isEqualTo(3);

    {
      final List<UniNode> children = decoder.decodeNodes(Bytes.of(nodes.get(1).getEncoding()));
      assertThat(children.size()).isEqualTo(3);
      assertThat(collectValues(children, storage::get))
          .containsExactlyInAnyOrder(Bytes.of(1), Bytes.of(2));
    }
    {
      final List<UniNode> children = decoder.decodeNodes(Bytes.of(nodes.get(2).getEncoding()));
      assertThat(children.size()).isEqualTo(3);
      assertThat(collectValues(children, storage::get))
          .containsExactlyInAnyOrder(
              Bytes.of(3), Bytes.fromHexString("0x" + Strings.repeat("bad", 50)));
    }
  }

  @Test
  public void breathFirstDecoder_fullTrie() {
    final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());

    // Build a small trie
    UniTrie<Bytes, Bytes> trie =
        new StoredUniTrie<>(storage::get, Function.identity(), Function.identity());

    trie.put(Bytes.of(0, 0), Bytes.of(1));
    trie.put(Bytes.of(0, 1), Bytes.of(2));
    trie.put(Bytes.of(1, 0), Bytes.of(3));

    // Create large leaf node that will not be inlined
    trie.put(
        Bytes.fromHexString("0x" + Strings.repeat("01", 32)),
        Bytes.fromHexString("0x" + Strings.repeat("bad", 50)));

    // Save nodes to storage
    trie.commit(storage::put, storage::put);

    // Decode 1st level (just root)
    final List<UniNode> depth0Nodes =
        UniTrieNodeDecoder.breadthFirstDecoder(storage::get, trie.getRootHash(), 0)
            .collect(Collectors.toList());

    assertThat(depth0Nodes.size()).isEqualTo(1);
    final UniNode rootNode = depth0Nodes.get(0);
    assertThat(Bytes32.wrap(rootNode.getHash())).isEqualTo(trie.getRootHash());

    // Decode first 2 levels
    final List<UniNode> depth0And1Nodes =
        UniTrieNodeDecoder.breadthFirstDecoder(storage::get, trie.getRootHash(), 1)
            .collect(Collectors.toList());

    final int secondLevelNodeCount = 2;
    final int expectedNodeCount = secondLevelNodeCount + 1;
    assertThat(depth0And1Nodes.size()).isEqualTo(expectedNodeCount);
    assertThat(depth0And1Nodes.get(0).getHash()).isEqualTo(rootNode.getHash());
    List<Bytes32> expectedNodesHashes = nonNullChildrenHashes(rootNode);
    List<Bytes32> actualNodeHashes =
        depth0And1Nodes.subList(1, expectedNodeCount).stream()
            .map(n -> Bytes32.wrap(n.getHash()))
            .collect(Collectors.toList());
    assertThat(actualNodeHashes).isEqualTo(expectedNodesHashes);

    // Decode full Unitrie
    final List<UniNode> allNodes =
        UniTrieNodeDecoder.breadthFirstDecoder(storage::get, trie.getRootHash())
            .collect(Collectors.toList());
    assertThat(allNodes.size()).isEqualTo(7);

    // Collect and check values
    List<Bytes> actualValues =
        allNodes.stream()
            .map(n -> n.getValue(storage::get))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Bytes::wrap)
            .collect(Collectors.toList());
    assertThat(actualValues)
        .containsExactly(
            Bytes.of(1),
            Bytes.of(2),
            Bytes.of(3),
            Bytes.fromHexString("0x" + Strings.repeat("bad", 50)));
  }

  @Test
  public void breadthFirstDecode_partialTrie() {
    final MerkleStorage fullStorage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
    final MerkleStorage partialStorage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());

    // Build a small trie
    UniTrie<Bytes, Bytes> trie =
        new StoredUniTrie<>(fullStorage::get, Function.identity(), Function.identity());

    LinearCongruentialGenerator g = new LinearCongruentialGenerator();
    for (int i = 0; i < 30; i++) {
      byte[] key = new byte[4];
      byte[] val = new byte[4];
      g.nextBytes(key);
      g.nextBytes(val);
      trie.put(Bytes.wrap(key), Bytes.wrap(val));
    }
    trie.commit(fullStorage::put, fullStorage::put);

    // Get root node
    UniNode rootNode =
        UniTrieNodeDecoder.breadthFirstDecoder(fullStorage::get, trie.getRootHash())
            .findFirst()
            .get();

    // Decode partially available trie
    partialStorage.put(trie.getRootHash(), Bytes.wrap(rootNode.getEncoding()));
    final List<UniNode> allDecodableNodes =
        UniTrieNodeDecoder.breadthFirstDecoder(partialStorage::get, trie.getRootHash())
            .collect(Collectors.toList());
    assertThat(allDecodableNodes.size()).isGreaterThanOrEqualTo(1);
    assertThat(allDecodableNodes.get(0).getHash()).isEqualTo(rootNode.getHash());
  }

  @Test
  public void breadthFirstDecode_emptyTrie() {
    List<UniNode> result =
        UniTrieNodeDecoder.breadthFirstDecoder(__ -> Optional.empty(), UniTrie.NULL_UNINODE_HASH)
            .collect(Collectors.toList());
    assertThat(result.size()).isEqualTo(0);
  }

  @Test
  public void breadthFirstDecode_singleNodeTrie() {
    final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());

    UniTrie<Bytes, Bytes> trie =
        new StoredUniTrie<>(storage::get, Function.identity(), Function.identity());
    trie.put(Bytes.fromHexString("0x100000"), Bytes.of(1));
    trie.commit(storage::put, storage::put);

    List<UniNode> result =
        UniTrieNodeDecoder.breadthFirstDecoder(storage::get, trie.getRootHash())
            .collect(Collectors.toList());
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getValue(storage::get)).contains(bytes(1));
    Bytes actualPath = PathEncoding.encodePath(Bytes.of(result.get(0).getPath()));
    assertThat(actualPath).isEqualTo(Bytes.fromHexString("0x100000"));
  }

  @Test
  public void breadthFirstDecode_unknownTrie() {
    Bytes32 randomRootHash = Bytes32.fromHexStringLenient("0x12");
    List<UniNode> result =
        UniTrieNodeDecoder.breadthFirstDecoder(__ -> Optional.empty(), randomRootHash)
            .collect(Collectors.toList());
    assertThat(result.size()).isEqualTo(0);
  }

  private List<Bytes> collectValues(final List<UniNode> nodes, final DataLoader loader) {
    return nodes.stream()
        .map(n -> n.getValue(loader))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Bytes::of)
        .collect(Collectors.toList());
  }

  private List<Bytes32> nonNullChildrenHashes(final UniNode node) {
    return Stream.of(node.getLeftChild(), node.getRightChild())
        .filter(n -> !Objects.equals(n, NullUniNode.instance()))
        .map(n -> Bytes32.wrap(n.getHash()))
        .collect(Collectors.toList());
  }

  private static class LinearCongruentialGenerator {
    final int m = 34783241;
    final int a = 6748319;
    final int c = 8736428;
    int seed = 984732957;

    int next() {
      seed = (a * seed + c) % m;
      return seed;
    }

    void nextBytes(final byte[] buffer) {
      int r = 0;
      for (int i = 0; i < buffer.length; i++) {
        if (i % Integer.BYTES == 0) {
          r = next();
        }
        buffer[i] = (byte) ((r >>> (8 * (i % Integer.BYTES))) & 0xff);
      }
    }
  }
}
