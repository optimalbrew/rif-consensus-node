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
package org.hyperledger.besu.ethereum.proof;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.trie.Proof;
import org.hyperledger.besu.ethereum.unitrie.StoredUniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrieKeyMapper;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.uint.UInt256;

public class UniTrieWorldStateProofProvider implements WorldStateProofProvider {

  private final WorldStateStorage worldStateStorage;
  private final UniTrieKeyMapper keyMapper;

  public UniTrieWorldStateProofProvider(final WorldStateStorage worldStateStorage) {
    this.worldStateStorage = worldStateStorage;
    this.keyMapper = new UniTrieKeyMapper();
  }

  @Override
  public Optional<WorldStateProof> getAccountProof(
      final Hash worldStateRoot,
      final Address accountAddress,
      final List<UInt256> accountStorageKeys) {

    if (!worldStateStorage.isWorldStateAvailable(worldStateRoot)) {
      return Optional.empty();
    } else {
      final UniTrie<BytesValue, BytesValue> trie = unitrie(worldStateRoot);

      final Proof<BytesValue> accountProof =
          trie.getValueWithProof(keyMapper.getAccountKey(accountAddress));

      return accountProof
          .getValue()
          .map(RLP::input)
          .map(StateTrieAccountValue::readFrom)
          .map(
              account -> {
                final SortedMap<UInt256, Proof<BytesValue>> storageProofs =
                    getStorageProofs(trie, accountAddress, accountStorageKeys);
                return new WorldStateProof(account, accountProof, storageProofs);
              });
    }
  }

  private SortedMap<UInt256, Proof<BytesValue>> getStorageProofs(
      final UniTrie<BytesValue, BytesValue> trie,
      final Address accountAddress,
      final List<UInt256> accountStorageKeys) {

    final SortedMap<UInt256, Proof<BytesValue>> storageProofs = new TreeMap<>();
    accountStorageKeys.forEach(key -> {
      BytesValue mappedKey = keyMapper.getAccountStorageKey(accountAddress, key);
      storageProofs.put(key, trie.getValueWithProof(mappedKey));
    });
    return storageProofs;
  }

  private UniTrie<BytesValue, BytesValue> unitrie(final Bytes32 rootHash) {
    return new StoredUniTrie<>(
        worldStateStorage::getAccountStateTrieNode, rootHash, b -> b, b -> b);
  }
}
