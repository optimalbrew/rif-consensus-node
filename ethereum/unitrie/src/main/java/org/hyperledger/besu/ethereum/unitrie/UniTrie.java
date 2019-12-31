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

import static org.hyperledger.besu.crypto.Hash.keccak256;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.trie.NodeUpdater;
import org.hyperledger.besu.ethereum.trie.Proof;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Interface for UniTrie data structure.
 */
public interface UniTrie<K, V> {

  BytesValue NULL_UNINODE_ENCODING = RLP.NULL;
  Bytes32 NULL_UNINODE_HASH = keccak256(NULL_UNINODE_ENCODING);

  /**
   * Get the sub UniTrie starting from the given key. Will be empty if key
   * is not present in this UniTrie.
   *
   * @param key key specifying root of subtree to return
   * @return  sub UniTrie, will be empty if key not present
   */
  UniTrie<K, V> getSubUniTrie(K key);

  /**
   * Returns an {@code Optional} of value mapped to the hash if it exists; otherwise empty.
   *
   * @param key The key for the value.
   * @return an {@code Optional} of value mapped to the hash if it exists; otherwise empty
   */
  Optional<V> get(K key);

  /**
   * Returns value and ordered proof-related nodes mapped to the hash if it exists; otherwise empty.
   *
   * @param key The key for the value.
   * @return value and ordered proof-related nodes
   */
  Proof<V> getValueWithProof(K key);

  /**
   * Updates the value mapped to the specified key, creating the mapping if one does not already
   * exist.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   */
  void put(K key, V value);

  /**
   * Deletes the value mapped to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   */
  void remove(K key);

  /**
   * Deletes the value mapped to the specified key, and all the values that are
   * below the given one in the underlying UniTrie storage.
   *
   * @param key  key specifying the root of the UniTrie subtree to remove.
   */
  void removeRecursive(K key);

  /**
   * Returns the KECCAK256 hash of the root node of the trie.
   *
   * @return The KECCAK256 hash of the root node of the trie.
   */
  Bytes32 getRootHash();

  /**
   * Commits any pending changes to the underlying storage.
   *
   * @param nodeUpdater used to store the node values
   */
  void commit(NodeUpdater nodeUpdater);

  /**
   * Visit all nodes in this unitrie.
   * @param visitor  unitrie visitor.
   */
  void visitAll(Consumer<UniNode> visitor);

  /**
   * Retrieve up to {@code limit} entries beginning from the first entry with path equal to
   * or greater than {@code starPath}.
   *
   * @param startPath the first path to return.
   * @param limit the maximum number of entries to return.
   * @return the requested entries as a map of entry path to entry value.
   */
  Map<BytesValue, V> entriesFrom(BytesValue startPath, int limit);
}
