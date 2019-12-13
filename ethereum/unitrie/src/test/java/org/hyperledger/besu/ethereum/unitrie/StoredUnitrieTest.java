/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 */
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.trie.StoredMerklePatriciaTrie;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class StoredUnitrieTest extends AbstractUnitrieTest {

    private KeyValueStorage keyValueStore;
    private MerkleStorage merkleStorage;
    private Function<String, BytesValue> valueSerializer;
    private Function<BytesValue, String> valueDeserializer;

    @Override
    MerklePatriciaTrie<BytesValue, String> createTrie() {
        keyValueStore = new InMemoryKeyValueStorage();
        merkleStorage = new KeyValueMerkleStorage(keyValueStore);
        valueSerializer = s -> Objects.isNull(s) ? null : BytesValue.wrap(s.getBytes(StandardCharsets.UTF_8));
        valueDeserializer = v -> new String(v.getArrayUnsafe(), StandardCharsets.UTF_8);
        return new StoredUnitrie<>(merkleStorage::get, valueSerializer, valueDeserializer);
    }

    @Test
    public void shouldDecodeCorrectly() {
        trie.put(BytesValue.fromHexString("0x0400"), "a");
        trie.put(BytesValue.fromHexString("0x0800"), "b");
        trie.commit(merkleStorage::put);

        // Ensure the extension branch can be loaded correct with its inlined child.
        final Bytes32 rootHash = trie.getRootHash();
        final StoredUnitrie<BytesValue, String> newTrie =
                new StoredUnitrie<>(merkleStorage::get, rootHash, valueSerializer, valueDeserializer);
        assertThat(newTrie.get(BytesValue.fromHexString("0x0800"))).contains("b");
    }

    @Test
    public void shouldDecodeCorrectlyAfterChanges() {
        trie.put(BytesValue.fromHexString("0x0400"), "a");
        trie.put(BytesValue.fromHexString("0x0800"), "b");
        trie.commit(merkleStorage::put);

        final Bytes32 rootHash = trie.getRootHash();
        final StoredUnitrie<BytesValue, String> newTrie =
                new StoredUnitrie<>(merkleStorage::get, rootHash, valueSerializer, valueDeserializer);

        newTrie.put(BytesValue.fromHexString("0x0800"), "c");
        final Bytes32 newHash = newTrie.getRootHash();
        newTrie.commit(merkleStorage::put);

        final StoredUnitrie<BytesValue, String> modifiedTrie =
                new StoredUnitrie<>(merkleStorage::get, newHash, valueSerializer, valueDeserializer);
        assertThat(modifiedTrie.get(BytesValue.fromHexString("0x0800"))).contains("c");
    }
}
