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
package org.hyperledger.besu.ethereum.worldstate;

import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.unitrie.StoredUniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.metrics.ObservableMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Mark-sweep storage collector based on UniTrie storage.
 *
 * @author ppedemon
 */
public class UniTrieMarkSweepPruner extends AbstractMarkSweepPruner {
  public UniTrieMarkSweepPruner(
      final WorldStateStorage worldStateStorage,
      final MutableBlockchain blockchain,
      final KeyValueStorage markStorage,
      final ObservableMetricsSystem metricsSystem) {

    super(worldStateStorage, blockchain, markStorage, metricsSystem);
  }

  public UniTrieMarkSweepPruner(
      final WorldStateStorage worldStateStorage,
      final MutableBlockchain blockchain,
      final KeyValueStorage markStorage,
      final ObservableMetricsSystem metricsSystem,
      final int operationsPerTransaction) {

    super(worldStateStorage, blockchain, markStorage, metricsSystem, operationsPerTransaction);
  }

  @Override
  public void mark(final Hash rootHash) {
    getMarkOperationCounter().inc();
    createStateTrie(rootHash)
        .visitAll(
            node -> {
              if (Thread.interrupted()) {
                // Since we don't expect to abort marking ourselves,
                // our abort process consists only of handling interrupts
                throw new RuntimeException("Interrupted while marking");
              }
              markNode(Bytes32.wrap(node.getHash()));
              if (node.getValueWrapper().isLong()) {
                node.getValueHash().ifPresent(h -> markNode(Bytes32.wrap(h)));
              }
            });
    LOG.debug("Completed marking used nodes for pruning");
  }

  private UniTrie<Bytes, Bytes> createStateTrie(final Bytes32 rootHash) {
    WorldStateStorage worldStateStorage = getWorldStateStorage();
    return new StoredUniTrie<>(
        worldStateStorage::getAccountStateTrieNode, rootHash, b -> b, b -> b);
  }
}
