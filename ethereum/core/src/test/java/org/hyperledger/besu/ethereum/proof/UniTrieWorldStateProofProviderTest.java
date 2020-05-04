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
package org.hyperledger.besu.ethereum.proof;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.unitrie.StoredUniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrieKeyMapper;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class UniTrieWorldStateProofProviderTest {

  private static final Address address =
      Address.fromHexString("0x1234567890123456789012345678901234567890");

  private KeyValueStorage kvStorage = new InMemoryKeyValueStorage();
  private WorldStateStorage worldStateStorage = new WorldStateKeyValueStorage(kvStorage);

  private UniTrieKeyMapper keyMapper = new UniTrieKeyMapper();

  private WorldStateProofProvider worldStateProofProvider;

  @Before
  public void setup() {
    worldStateProofProvider = new UniTrieWorldStateProofProvider(worldStateStorage);
  }

  @Test
  public void getProofWhenWorldStateNotAvailable() {
    Optional<WorldStateProof> accountProof =
        worldStateProofProvider.getAccountProof(Hash.EMPTY, address, Collections.emptyList());

    assertThat(accountProof).isEmpty();
  }

  @Test
  public void getProofWhenWorldStateAvailable() {
    final UniTrie<Bytes, Bytes> trie = unitrie();
    Hash storageRoot = prepareForStorage(trie, address);

    // Add some storage values
    writeStorageValue(trie, address, UInt256.of(1L), UInt256.of(2L));
    writeStorageValue(trie, address, UInt256.of(2L), UInt256.of(4L));
    writeStorageValue(trie, address, UInt256.of(3L), UInt256.of(6L));

    WorldStateStorage.Updater updater = worldStateStorage.updater();
    trie.commit(updater::putAccountStateTrieNode, updater::rawPut);
    updater.commit();

    // Add account
    final Hash codeHash = Hash.hash(Bytes.fromHexString("0x1122"));
    final StateTrieAccountValue accountValue =
        new StateTrieAccountValue(1L, Wei.of(2L), storageRoot, codeHash, 0);
    trie.put(keyMapper.getAccountKey(address), RLP.encode(accountValue::writeTo));

    updater = worldStateStorage.updater();
    trie.commit(updater::putAccountStateTrieNode, updater::rawPut);
    updater.commit();

    final List<UInt256> storageKeys = Arrays.asList(UInt256.of(1L), UInt256.of(3L), UInt256.of(6L));
    final Optional<WorldStateProof> accountProof =
        worldStateProofProvider.getAccountProof(
            Hash.wrap(trie.getRootHash()), address, storageKeys);

    assertThat(accountProof).isPresent();
    assertThat(accountProof.get().getStateTrieAccountValue())
        .isEqualToComparingFieldByField(accountValue);
    assertThat(accountProof.get().getAccountProof().size()).isGreaterThanOrEqualTo(1);
    // Check storage fields
    assertThat(accountProof.get().getStorageKeys()).isEqualTo(storageKeys);
    // Check key 1
    UInt256 storageKey = UInt256.of(1L);
    assertThat(accountProof.get().getStorageValue(storageKey)).isEqualTo(UInt256.of(2L));
    assertThat(accountProof.get().getStorageProof(storageKey).size()).isGreaterThanOrEqualTo(1);
    // Check key 3
    storageKey = UInt256.of(3L);
    assertThat(accountProof.get().getStorageValue(storageKey)).isEqualTo(UInt256.of(6L));
    assertThat(accountProof.get().getStorageProof(storageKey).size()).isGreaterThanOrEqualTo(1);
    // Check key 6
    storageKey = UInt256.of(6L);
    assertThat(accountProof.get().getStorageValue(storageKey)).isEqualTo(UInt256.of(0L));
    assertThat(accountProof.get().getStorageProof(storageKey).size()).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void getProofWhenStateTrieAccountUnavailable() {
    final UniTrie<Bytes, Bytes> worldStateTrie = unitrie();

    final Optional<WorldStateProof> accountProof =
        worldStateProofProvider.getAccountProof(
            Hash.wrap(worldStateTrie.getRootHash()), address, Collections.emptyList());

    assertThat(accountProof).isEmpty();
  }

  private Hash prepareForStorage(final UniTrie<Bytes, Bytes> trie, final Address address) {
    Bytes storagePrefixKey = keyMapper.getAccountStoragePrefixKey(address);
    trie.put(storagePrefixKey, Bytes.of(0));
    return Hash.wrap(trie.getHash(storagePrefixKey));
  }

  private void writeStorageValue(
      final UniTrie<Bytes, Bytes> trie,
      final Address address,
      final UInt256 key,
      final UInt256 value) {
    Bytes storageKey = keyMapper.getAccountStorageKey(address, key);
    trie.put(storageKey, encodeStorageValue(value));
  }

  private Bytes encodeStorageValue(final UInt256 storageValue) {
    return RLP.encode(out -> out.writeUInt256Scalar(storageValue));
  }

  private UniTrie<Bytes, Bytes> unitrie() {
    return new StoredUniTrie<>(worldStateStorage::getAccountStateTrieNode, b -> b, b -> b);
  }
}
