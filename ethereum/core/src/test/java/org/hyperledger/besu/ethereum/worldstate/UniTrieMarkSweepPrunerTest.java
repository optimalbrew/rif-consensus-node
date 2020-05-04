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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.core.InMemoryStorageProvider.createInMemoryBlockchain;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;

import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.BlockDataGenerator;
import org.hyperledger.besu.ethereum.core.BlockDataGenerator.BlockOptions;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.TransactionReceipt;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.merkleutils.UniTrieMerkleAwareProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.hyperledger.besu.ethereum.unitrie.StoredUniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.InOrder;

public class UniTrieMarkSweepPrunerTest {

  // Block generation
  private final BlockDataGenerator gen = new BlockDataGenerator();
  private final Block genesisBlock = gen.genesisBlock();
  private final MutableBlockchain blockchain = createInMemoryBlockchain(genesisBlock);

  // World state
  private final Map<Bytes, byte[]> hashValueStore = spy(new HashMap<>());
  private final InMemoryKeyValueStorage stateStorage = spy(new TestInMemoryStorage(hashValueStore));
  private final WorldStateStorage worldStateStorage = new WorldStateKeyValueStorage(stateStorage);

  // World state archive
  private final WorldStateArchive worldStateArchive =
      new WorldStateArchive(
          worldStateStorage,
          new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage()),
          new UniTrieMerkleAwareProvider());

  // Pruner dependencies
  private final NoOpMetricsSystem metricsSystem = new NoOpMetricsSystem();
  private final InMemoryKeyValueStorage markStorage = new InMemoryKeyValueStorage();

  @Test
  public void mark_marksAllExpectedNodes() {
    final UniTrieMarkSweepPruner pruner =
        new UniTrieMarkSweepPruner(worldStateStorage, blockchain, markStorage, metricsSystem);

    // Generate accounts and save corresponding state root
    final int numBlocks = 15;
    final int numAccounts = 10;
    List<Account> accounts = generateBlockchainData(numBlocks, numAccounts);

    final int markBlockNumber = 10;
    final BlockHeader markBlock = blockchain.getBlockHeader(markBlockNumber).get();

    // Collect the nodes we expect to keep
    final Set<Bytes> expectedNodes = collectUniTrieNodes(markBlock.getStateRoot());
    assertThat(hashValueStore.size()).isGreaterThan(expectedNodes.size());

    // Mark and sweep
    pruner.mark(markBlock.getStateRoot());
    pruner.sweepBefore(markBlock.getNumber());

    // Assert that the block we marked is still present and all accounts are accessible
    WorldState markedState =
        worldStateArchive
            .get(markBlock.getStateRoot())
            .orElseGet(
                () -> Assertions.fail("No world state for hash = %s", markBlock.getStateRoot()));

    // Verify we have the expected accounts
    verifyAccounts(markedState, accounts.subList(0, numAccounts * markBlockNumber));

    // All other state roots should have been removed
    for (int i = 0; i < numBlocks; i++) {
      final BlockHeader curHeader = blockchain.getBlockHeader(i + 1L).get();
      if (curHeader.getNumber() == markBlock.getNumber()) {
        continue;
      }
      assertThat(worldStateArchive.get(curHeader.getStateRoot())).isEmpty();
    }

    // Check that storage contains only the values we expect
    assertThat(hashValueStore.size()).isEqualTo(expectedNodes.size());
    assertThat(hashValueStore.values())
        .containsExactlyInAnyOrderElementsOf(
            expectedNodes.stream().map(Bytes::getArrayUnsafe).collect(Collectors.toSet()));
  }

  @Test
  public void sweepBefore_shouldSweepStateRootFirst() {
    final UniTrieMarkSweepPruner pruner =
        new UniTrieMarkSweepPruner(worldStateStorage, blockchain, markStorage, metricsSystem, 1);

    // Generate accounts and save corresponding state root
    final int numBlocks = 15;
    final int numAccounts = 10;
    generateBlockchainData(numBlocks, numAccounts);

    final int markBlockNumber = 10;
    final BlockHeader markBlock = blockchain.getBlockHeader(markBlockNumber).get();

    // Collect state roots we expect to be swept first
    final List<Bytes32> stateRoots = new ArrayList<>();
    for (int i = markBlockNumber - 1; i >= 0; i--) {
      stateRoots.add(blockchain.getBlockHeader(i).get().getStateRoot());
    }

    // Mark and sweep
    pruner.mark(markBlock.getStateRoot());
    pruner.sweepBefore(markBlock.getNumber());

    // Check stateRoots are marked first
    InOrder inOrder = inOrder(hashValueStore, stateStorage);
    for (Bytes32 stateRoot : stateRoots) {
      inOrder.verify(hashValueStore).remove(stateRoot);
    }
    inOrder.verify(stateStorage).removeAllKeysUnless(any());
  }

  @Test
  public void sweepBefore_shouldNotRemoveMarkedStateRoots() {
    final UniTrieMarkSweepPruner pruner =
        new UniTrieMarkSweepPruner(worldStateStorage, blockchain, markStorage, metricsSystem, 1);

    // Generate accounts and save corresponding state root
    final int numBlocks = 15;
    final int numAccounts = 10;
    generateBlockchainData(numBlocks, numAccounts);

    final int markBlockNumber = 10;
    final BlockHeader markBlock = blockchain.getBlockHeader(markBlockNumber).get();

    // Collect state roots we expect to be swept first
    final List<Bytes32> stateRoots = new ArrayList<>();
    for (int i = markBlockNumber - 1; i >= 0; i--) {
      stateRoots.add(blockchain.getBlockHeader(i).get().getStateRoot());
    }

    // Mark
    pruner.mark(markBlock.getStateRoot());
    // Mark an extra state root
    Hash markedRoot = Hash.wrap(stateRoots.remove(stateRoots.size() / 2));
    pruner.markNode(markedRoot);
    // Sweep
    pruner.sweepBefore(markBlock.getNumber());

    // Check stateRoots are marked first
    InOrder inOrder = inOrder(hashValueStore, stateStorage);
    for (Bytes32 stateRoot : stateRoots) {
      inOrder.verify(hashValueStore).remove(stateRoot);
    }
    inOrder.verify(stateStorage).removeAllKeysUnless(any());

    assertThat(stateStorage.containsKey(markedRoot.getArrayUnsafe())).isTrue();
  }

  private List<Account> generateBlockchainData(final int numBlocks, final int numAccounts) {
    List<List<Account>> accountsPerBlock = new ArrayList<>();

    Block parentBlock = blockchain.getChainHeadBlock();
    for (int i = 0; i < numBlocks; i++) {
      final MutableWorldState worldState =
          worldStateArchive.getMutable(parentBlock.getHeader().getStateRoot()).get();

      List<Account> accounts =
          gen.createRandomContractAccountsWithNonEmptyStorage(worldState, numAccounts);
      accountsPerBlock.add(accounts);

      final Hash stateRoot = worldState.rootHash();

      final Block block =
          gen.block(
              BlockOptions.create()
                  .setStateRoot(stateRoot)
                  .setBlockNumber(parentBlock.getHeader().getNumber() + 1L)
                  .setParentHash(parentBlock.getHash()));
      final List<TransactionReceipt> receipts = gen.receipts(block);
      blockchain.appendBlock(block, receipts);
      parentBlock = block;
    }

    return accountsPerBlock.stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  private void verifyAccounts(final WorldState state, final List<Account> accounts) {
    for (Account account : accounts) {
      if (state.get(account.getAddress()) == null) {
        Assertions.fail("State doesn't have account: %s", account.getAddress());
      }
    }
  }

  private Set<Bytes> collectUniTrieNodes(final Hash stateRoot) {
    Set<Bytes> collector = new HashSet<>();
    collectUniTrieNodes(createUniTrie(stateRoot), collector);
    return collector;
  }

  private void collectUniTrieNodes(final UniTrie<Bytes, Bytes> trie, final Set<Bytes> collector) {

    final Bytes32 rootHash = trie.getRootHash();
    trie.visitAll(
        node -> {
          if (node.getValueWrapper().isLong()) {
            node.getValue(worldStateStorage::getAccountStateTrieNode)
                .ifPresent(v -> collector.add(Bytes.of(v)));
          }
          if (node.isReferencedByHash() || Bytes32.wrap(node.getHash()).equals(rootHash)) {
            collector.add(Bytes.of(node.getEncoding()));
          }
        });
  }

  private UniTrie<Bytes, Bytes> createUniTrie(final Bytes32 rootHash) {
    return new StoredUniTrie<>(
        worldStateStorage::getAccountStateTrieNode,
        rootHash,
        Function.identity(),
        Function.identity());
  }

  /** Proxy class so we can access to the constructor that takes a map */
  private static class TestInMemoryStorage extends InMemoryKeyValueStorage {
    TestInMemoryStorage(final Map<Bytes, byte[]> hashValueStore) {
      super(hashValueStore);
    }
  }
}
