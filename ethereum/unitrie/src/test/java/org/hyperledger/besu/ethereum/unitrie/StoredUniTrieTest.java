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

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

public class StoredUniTrieTest extends AbstractUniTrieTest {

  private KeyValueStorage keyValueStore;
  private MerkleStorage merkleStorage;
  private Function<String, BytesValue> valueSerializer;
  private Function<BytesValue, String> valueDeserializer;

  @Override
  UniTrie<BytesValue, String> createTrie() {
    keyValueStore = new InMemoryKeyValueStorage();
    merkleStorage = new KeyValueMerkleStorage(keyValueStore);
    valueSerializer =
        s -> Objects.isNull(s) ? null : BytesValue.wrap(s.getBytes(StandardCharsets.UTF_8));
    valueDeserializer = v -> new String(v.getArrayUnsafe(), StandardCharsets.UTF_8);
    return new StoredUniTrie<>(merkleStorage::get, valueSerializer, valueDeserializer);
  }

  @Test
  public void shouldDecodeCorrectly() {
    trie.put(BytesValue.fromHexString("0x0400"), "a");
    trie.put(BytesValue.fromHexString("0x0800"), "b");
    trie.commit(merkleStorage::put, merkleStorage::put);

    final Bytes32 rootHash = trie.getRootHash();
    final StoredUniTrie<BytesValue, String> newTrie =
        new StoredUniTrie<>(merkleStorage::get, rootHash, valueSerializer, valueDeserializer);
    assertThat(newTrie.get(BytesValue.fromHexString("0x0800"))).contains("b");
  }

  @Test
  public void shouldDecodeCorrectlyAfterChanges() {
    trie.put(BytesValue.fromHexString("0x0400"), "a");
    trie.put(BytesValue.fromHexString("0x0800"), "b");
    trie.commit(merkleStorage::put, merkleStorage::put);

    final Bytes32 rootHash = trie.getRootHash();
    final StoredUniTrie<BytesValue, String> newTrie =
        new StoredUniTrie<>(merkleStorage::get, rootHash, valueSerializer, valueDeserializer);

    newTrie.put(BytesValue.fromHexString("0x0800"), "c");
    final Bytes32 newHash = newTrie.getRootHash();
    newTrie.commit(merkleStorage::put, merkleStorage::put);

    final StoredUniTrie<BytesValue, String> modifiedTrie =
        new StoredUniTrie<>(merkleStorage::get, newHash, valueSerializer, valueDeserializer);
    assertThat(modifiedTrie.get(BytesValue.fromHexString("0x0800"))).contains("c");
  }

  @Test
  public void canReloadTrieFromHash() {
    final BytesValue key1 = BytesValue.of(1, 5, 8, 9);
    final BytesValue key2 = BytesValue.of(1, 6, 1, 2);
    final BytesValue key3 = BytesValue.of(1, 6, 1, 3);

    // Push some values into the trie and commit changes so nodes are persisted
    final String value1 = "value1";
    trie.put(key1, value1);
    final Bytes32 hash1 = trie.getRootHash();
    trie.commit(merkleStorage::put, merkleStorage::put);

    final String value2 = "value2";
    trie.put(key2, value2);
    final String value3 = "value3";
    trie.put(key3, value3);
    final Bytes32 hash2 = trie.getRootHash();
    trie.commit(merkleStorage::put, merkleStorage::put);

    final String value4 = "value4";
    trie.put(key1, value4);
    final Bytes32 hash3 = trie.getRootHash();
    trie.commit(merkleStorage::put, merkleStorage::put);

    // Check the root hashes for 3 tries are all distinct
    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(hash1).isNotEqualTo(hash3);
    assertThat(hash2).isNotEqualTo(hash3);
    // And that we can retrieve the last value we set for key1
    assertThat(trie.get(key1)).contains("value4");

    // Create new tries from root hashes and check that we find expected values
    trie = new StoredUniTrie<>(merkleStorage::get, hash1, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).contains("value1");
    assertThat(trie.get(key2)).isEmpty();
    assertThat(trie.get(key3)).isEmpty();

    trie = new StoredUniTrie<>(merkleStorage::get, hash2, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).contains("value1");
    assertThat(trie.get(key2)).contains("value2");
    assertThat(trie.get(key3)).contains("value3");

    trie = new StoredUniTrie<>(merkleStorage::get, hash3, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).contains("value4");
    assertThat(trie.get(key2)).contains("value2");
    assertThat(trie.get(key3)).contains("value3");

    // Commit changes to storage, and create new tries from roothash and new storage instance
    merkleStorage.commit();
    final MerkleStorage newMerkleStorage = new KeyValueMerkleStorage(keyValueStore);
    trie = new StoredUniTrie<>(newMerkleStorage::get, hash1, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).contains("value1");
    assertThat(trie.get(key2)).isEmpty();
    assertThat(trie.get(key3)).isEmpty();

    trie = new StoredUniTrie<>(newMerkleStorage::get, hash2, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).contains("value1");
    assertThat(trie.get(key2)).contains("value2");
    assertThat(trie.get(key3)).contains("value3");

    trie = new StoredUniTrie<>(newMerkleStorage::get, hash3, valueSerializer, valueDeserializer);
    assertThat(trie.get(key1)).contains("value4");
    assertThat(trie.get(key2)).contains("value2");
    assertThat(trie.get(key3)).contains("value3");
  }

  @Test
  public void saveAndReloadLargeTrie() {
    for (int i = 0; i < 1000; i++) {
      trie.put(toKey(i), toValue(i));
    }
    trie.commit(merkleStorage::put, merkleStorage::put);

    UniTrie<BytesValue, String> loadedTrie =
        new StoredUniTrie<>(
            merkleStorage::get, trie.getRootHash(), valueSerializer, valueDeserializer);
    for (int i = 0; i < 1000; i++) {
      BytesValue key = toKey(i);
      assertThat(loadedTrie.get(key)).isEqualTo(trie.get(key));
    }
  }

  private BytesValue toKey(final int i) {
    Bytes32 hash =
        Hash.keccak256(BytesValue.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8)));
    return BytesValue.wrap(hash.getArrayUnsafe());
  }

  private String toValue(final int i) {
    if ((i & 1) == 0) {
      return String.valueOf(i);
    } else {
      Bytes32 hash =
          Hash.keccak256(BytesValue.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8)));
      return hash.toUnprefixedString();
    }
  }
}
