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
package org.hyperledger.besu.ethereum.worldstate;

import java.util.stream.Stream;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.unitrie.StoredUniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrieKeyMapper;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

@SuppressWarnings("unused")
public class UniTrieMutableWorldState implements MutableWorldState {

  private final WorldStateStorage worldStateStorage;
  private final WorldStatePreimageStorage preimageStorage;

  private final UniTrie<BytesValue, BytesValue> trie;
  private final UniTrieKeyMapper keyMapper = new UniTrieKeyMapper();

  public UniTrieMutableWorldState(
      final WorldStateStorage storage, final WorldStatePreimageStorage preimageStorage) {
    this(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH, storage, preimageStorage);
  }

  public UniTrieMutableWorldState(
      final Bytes32 rootHash,
      final WorldStateStorage worldStateStorage,
      final WorldStatePreimageStorage preimageStorage) {
    this.worldStateStorage = worldStateStorage;
    this.preimageStorage = preimageStorage;
    this.trie = initTrie(rootHash);
  }

  public UniTrieMutableWorldState(final WorldState worldState) {
    if (!(worldState instanceof UniTrieMutableWorldState)) {
      throw new UnsupportedOperationException();
    }

    final UniTrieMutableWorldState other = (UniTrieMutableWorldState) worldState;
    this.worldStateStorage = other.worldStateStorage;
    this.preimageStorage = other.preimageStorage;
    this.trie = initTrie(other.trie.getRootHash());
  }

  private UniTrie<BytesValue, BytesValue> initTrie(final Bytes32 rootHash) {
    return new StoredUniTrie<>(
        worldStateStorage::getAccountStateTrieNode, rootHash, b -> b, b -> b);
  }

  @Override
  public Hash rootHash() {
    return Hash.wrap(trie.getRootHash());
  }

  @Override
  public MutableWorldState copy() {
    return new UniTrieMutableWorldState(rootHash(), worldStateStorage, preimageStorage);
  }

  @Override
  public void persist() {

  }

  @Override
  public WorldUpdater updater() {
    return null;
  }

  @Override
  public Stream<StreamableAccount> streamAccounts(final Bytes32 startKeyHash, final int limit) {
    return null;
  }

  @Override
  public Account get(final Address address) {
    return null;
  }
}
