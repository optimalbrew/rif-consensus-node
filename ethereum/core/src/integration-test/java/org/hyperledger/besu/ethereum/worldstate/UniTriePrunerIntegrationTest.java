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
import org.hyperledger.besu.ethereum.worldstate.Pruner.PruningPhase;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.testutil.MockExecutorService;

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

public class UniTriePrunerIntegrationTest {

  // Block generation
  private final BlockDataGenerator gen = new BlockDataGenerator();
  private final Block genesisBlock = gen.genesisBlock();
  private final MutableBlockchain blockchain = createInMemoryBlockchain(genesisBlock);

  // World state
  private final Map<Bytes, byte[]> hashValueStore = new HashMap<>();
  private final InMemoryKeyValueStorage stateStorage = new TestInMemoryStorage(hashValueStore);
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
  public void pruner_smallState_manyOpsPerTx() throws InterruptedException {
    testPruner(3, 1, 1, 4, 1000);
  }

  @Test
  public void pruner_largeState_fewOpsPerTx() throws InterruptedException {
    testPruner(2, 5, 5, 6, 5);
  }

  @Test
  public void pruner_emptyBlocks() throws InterruptedException {
    testPruner(5, 0, 2, 5, 10);
  }

  @Test
  public void pruner_markChainhead() throws InterruptedException {
    testPruner(4, 2, 1, 10, 20);
  }

  @Test
  public void pruner_lowRelativeBlockConfirmations() throws InterruptedException {
    testPruner(3, 2, 1, 4, 20);
  }

  @Test
  public void pruner_highRelativeBlockConfirmations() throws InterruptedException {
    testPruner(3, 2, 9, 10, 20);
  }

  private void testPruner(
      final int numCycles,
      final int accountsPerBlock,
      final int blockConfirmations,
      final int numBlocksToKeep,
      final int opsPerTransaction) {

    final var markSweepPruner =
        new UniTrieMarkSweepPruner(
            worldStateStorage, blockchain, markStorage, metricsSystem, opsPerTransaction);
    final var pruner =
        new Pruner(
            markSweepPruner,
            blockchain,
            new PrunerConfiguration(blockConfirmations, numBlocksToKeep),
            MockExecutorService::new);

    pruner.start();

    List<Account> allGeneratedAccounts = new ArrayList<>();

    for (int cycle = 0; cycle < numCycles; ++cycle) {
      int numBlockInCycle =
          numBlocksToKeep
              + 1; // +1 to get it to switch from MARKING_COMPLETE TO SWEEPING on each cycle
      var fullyMarkedBlockNum = cycle * numBlockInCycle + 1;

      // This should cause a full mark and sweep cycle
      assertThat(pruner.getPruningPhase()).isEqualByComparingTo(PruningPhase.IDLE);
      List<Account> generatedAccounts = generateBlockchainData(numBlockInCycle, accountsPerBlock);
      allGeneratedAccounts.addAll(generatedAccounts);
      assertThat(pruner.getPruningPhase()).isEqualByComparingTo(PruningPhase.IDLE);

      // Collect the nodes we expect to keep
      final Set<Bytes> expectedNodes = new HashSet<>();
      for (int i = fullyMarkedBlockNum; i <= blockchain.getChainHeadBlockNumber(); i++) {
        final Hash stateRoot = blockchain.getBlockHeader(i).get().getStateRoot();
        collectUniTrieNodes(stateRoot, expectedNodes);
      }

      if (accountsPerBlock != 0) {
        assertThat(hashValueStore.size()).isGreaterThanOrEqualTo(expectedNodes.size());
      }

      // Assert that blocks from mark point onward are still accessible
      for (int i = fullyMarkedBlockNum; i <= blockchain.getChainHeadBlockNumber(); i++) {
        final Hash stateRoot = blockchain.getBlockHeader(i).get().getStateRoot();
        assertThat(worldStateArchive.get(stateRoot)).isPresent();
        final WorldState markedState = worldStateArchive.get(stateRoot).get();
        verifyAccounts(markedState, allGeneratedAccounts.subList(0, accountsPerBlock * i));
      }

      // All other state roots should have been removed
      for (int i = 0; i < fullyMarkedBlockNum; i++) {
        final BlockHeader curHeader = blockchain.getBlockHeader(i).get();
        if (!curHeader.getStateRoot().equals(Hash.EMPTY_TRIE_HASH)) {
          assertThat(worldStateArchive.get(curHeader.getStateRoot())).isEmpty();
        }
      }

      // Check that storage contains only the values we expect
      assertThat(hashValueStore.size()).isEqualTo(expectedNodes.size());
      assertThat(hashValueStore.values())
          .containsExactlyInAnyOrderElementsOf(
              expectedNodes.stream().map(Bytes::toArrayUnsafe).collect(Collectors.toSet()));
    }

    pruner.stop();
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

  private void collectUniTrieNodes(final Hash stateRoot, final Set<Bytes> collector) {
    UniTrie<Bytes, Bytes> trie = createUniTrie(stateRoot);
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
