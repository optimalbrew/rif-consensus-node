/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.consensus.clique.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.pantheon.ethereum.core.InMemoryStorageProvider.createInMemoryBlockchain;

import tech.pegasys.pantheon.consensus.clique.CliqueContext;
import tech.pegasys.pantheon.consensus.clique.CliqueMiningTracker;
import tech.pegasys.pantheon.consensus.clique.TestHelpers;
import tech.pegasys.pantheon.consensus.common.VoteTally;
import tech.pegasys.pantheon.consensus.common.VoteTallyCache;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockBody;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderTestFixture;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.Util;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncState;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CliqueMiningCoordinatorTest {

  private final KeyPair proposerKeys = KeyPair.generate();
  private final KeyPair validatorKeys = KeyPair.generate();
  private final Address proposerAddress = Util.publicKeyToAddress(proposerKeys.getPublicKey());
  private final Address validatorAddress = Util.publicKeyToAddress(validatorKeys.getPublicKey());

  private final List<Address> validators = Lists.newArrayList(validatorAddress, proposerAddress);

  private final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();

  private CliqueMiningTracker miningTracker;

  @Mock private MutableBlockchain blockChain;
  @Mock private ProtocolContext<CliqueContext> protocolContext;
  @Mock private CliqueMinerExecutor minerExecutor;
  @Mock private CliqueBlockMiner blockMiner;
  @Mock private SyncState syncState;
  @Mock private VoteTallyCache voteTallyCache;

  @Before
  public void setup() {

    headerTestFixture.number(1);
    Block genesisBlock = createEmptyBlock(0, Hash.ZERO, proposerKeys); // not normally signed but ok
    blockChain = createInMemoryBlockchain(genesisBlock);

    final VoteTally voteTally = mock(VoteTally.class);
    when(voteTally.getValidators()).thenReturn(validators);
    when(voteTallyCache.getVoteTallyAfterBlock(any())).thenReturn(voteTally);
    final CliqueContext cliqueContext = new CliqueContext(voteTallyCache, null, null);

    when(protocolContext.getConsensusState()).thenReturn(cliqueContext);
    when(protocolContext.getBlockchain()).thenReturn(blockChain);
    when(minerExecutor.startAsyncMining(any(), any())).thenReturn(blockMiner);
    when(syncState.isInSync()).thenReturn(true);

    miningTracker = new CliqueMiningTracker(proposerAddress, protocolContext);
  }

  @Test
  public void outOfTurnBlockImportedDoesNotInterruptInTurnMiningOperation() {
    // As the head of the blockChain is 0 (which effectively doesn't have a signer, all validators
    // are able to propose.

    when(blockMiner.getParentHeader()).thenReturn(blockChain.getChainHeadHeader());

    // Note also - validators is an hard-ordered LIST, thus in-turn will follow said list - block_1
    // should be created by proposer.
    final CliqueMiningCoordinator coordinator =
        new CliqueMiningCoordinator(blockChain, minerExecutor, syncState, miningTracker);

    coordinator.enable();

    verify(minerExecutor, times(1)).startAsyncMining(any(), any());

    reset(minerExecutor);

    final Block importedBlock = createEmptyBlock(1, blockChain.getChainHeadHash(), validatorKeys);

    blockChain.appendBlock(importedBlock, Lists.emptyList());

    // The minerExecutor should not be invoked as the mining operation was conducted by an in-turn
    // validator, and the created block came from an out-turn validator.
    verify(minerExecutor, never()).startAsyncMining(any(), any());
  }

  @Test
  public void outOfTurnBlockImportedAtHigherLevelInterruptsMiningOperation() {
    // As the head of the blockChain is 1 (which effectively doesn't have a signer, all validators
    // are able to propose.
    when(blockMiner.getParentHeader()).thenReturn(blockChain.getChainHeadHeader());

    // Note also - validators is an hard-ordered LIST, thus in-turn will follow said list - block_1
    // should be created by proposer.
    final CliqueMiningCoordinator coordinator =
        new CliqueMiningCoordinator(blockChain, minerExecutor, syncState, miningTracker);

    coordinator.enable();

    verify(minerExecutor, times(1)).startAsyncMining(any(), any());

    reset(minerExecutor);
    when(minerExecutor.startAsyncMining(any(), any())).thenReturn(blockMiner);

    final Block importedBlock = createEmptyBlock(2, blockChain.getChainHeadHash(), validatorKeys);

    blockChain.appendBlock(importedBlock, Lists.emptyList());

    // The minerExecutor should not be invoked as the mining operation was conducted by an in-turn
    // validator, and the created block came from an out-turn validator.
    ArgumentCaptor<BlockHeader> varArgs = ArgumentCaptor.forClass(BlockHeader.class);
    verify(minerExecutor, times(1)).startAsyncMining(any(), varArgs.capture());
    assertThat(varArgs.getValue()).isEqualTo(blockChain.getChainHeadHeader());
  }

  @Test
  public void outOfTurnBlockImportedInterruptsOutOfTurnMiningOperation() {
    blockChain.appendBlock(
        createEmptyBlock(1, blockChain.getChainHeadHash(), validatorKeys), Lists.emptyList());

    when(blockMiner.getParentHeader()).thenReturn(blockChain.getChainHeadHeader());

    // Note also - validators is an hard-ordered LIST, thus in-turn will follow said list - block_2
    // should be created by 'validator', thus Proposer is out-of-turn.
    final CliqueMiningCoordinator coordinator =
        new CliqueMiningCoordinator(blockChain, minerExecutor, syncState, miningTracker);

    coordinator.enable();

    verify(minerExecutor, times(1)).startAsyncMining(any(), any());

    reset(minerExecutor);
    when(minerExecutor.startAsyncMining(any(), any())).thenReturn(blockMiner);

    final Block importedBlock = createEmptyBlock(2, blockChain.getChainHeadHash(), validatorKeys);

    blockChain.appendBlock(importedBlock, Lists.emptyList());

    // The minerExecutor should not be invoked as the mining operation was conducted by an in-turn
    // validator, and the created block came from an out-turn validator.
    ArgumentCaptor<BlockHeader> varArgs = ArgumentCaptor.forClass(BlockHeader.class);
    verify(minerExecutor, times(1)).startAsyncMining(any(), varArgs.capture());
    assertThat(varArgs.getValue()).isEqualTo(blockChain.getChainHeadHeader());
  }

  @Test
  public void outOfTurnBlockImportedInterruptsNonRunningMiner() {
    blockChain.appendBlock(
        createEmptyBlock(1, blockChain.getChainHeadHash(), proposerKeys), Lists.emptyList());

    when(blockMiner.getParentHeader()).thenReturn(blockChain.getChainHeadHeader());

    // Note also - validators is an hard-ordered LIST, thus in-turn will follow said list - block_2
    // should be created by 'validator', thus Proposer is out-of-turn.
    final CliqueMiningCoordinator coordinator =
        new CliqueMiningCoordinator(blockChain, minerExecutor, syncState, miningTracker);

    coordinator.enable();

    verify(minerExecutor, times(1)).startAsyncMining(any(), any());

    reset(minerExecutor);
    when(minerExecutor.startAsyncMining(any(), any())).thenReturn(blockMiner);

    final Block importedBlock = createEmptyBlock(2, blockChain.getChainHeadHash(), validatorKeys);

    blockChain.appendBlock(importedBlock, Lists.emptyList());

    // The minerExecutor should not be invoked as the mining operation was conducted by an in-turn
    // validator, and the created block came from an out-turn validator.
    ArgumentCaptor<BlockHeader> varArgs = ArgumentCaptor.forClass(BlockHeader.class);
    verify(minerExecutor, times(1)).startAsyncMining(any(), varArgs.capture());
    assertThat(varArgs.getValue()).isEqualTo(blockChain.getChainHeadHeader());
  }

  @Test
  public void locallyGeneratedBlockInvalidatesMiningEvenIfInTurn() {
    // Note also - validators is an hard-ordered LIST, thus in-turn will follow said list - block_1
    // should be created by Proposer, and thus will be in-turn.
    final CliqueMiningCoordinator coordinator =
        new CliqueMiningCoordinator(blockChain, minerExecutor, syncState, miningTracker);

    coordinator.enable();

    verify(minerExecutor, times(1)).startAsyncMining(any(), any());

    reset(minerExecutor);
    when(minerExecutor.startAsyncMining(any(), any())).thenReturn(blockMiner);

    final Block importedBlock = createEmptyBlock(1, blockChain.getChainHeadHash(), proposerKeys);
    blockChain.appendBlock(importedBlock, Lists.emptyList());

    // The minerExecutor should not be invoked as the mining operation was conducted by an in-turn
    // validator, and the created block came from an out-turn validator.
    ArgumentCaptor<BlockHeader> varArgs = ArgumentCaptor.forClass(BlockHeader.class);
    verify(minerExecutor, times(1)).startAsyncMining(any(), varArgs.capture());
    assertThat(varArgs.getValue()).isEqualTo(blockChain.getChainHeadHeader());
  }

  private Block createEmptyBlock(
      final long blockNumber, final Hash parentHash, final KeyPair signer) {
    headerTestFixture.number(blockNumber).parentHash(parentHash);
    final BlockHeader header =
        TestHelpers.createCliqueSignedBlockHeader(headerTestFixture, signer, validators);
    return new Block(header, new BlockBody(Lists.emptyList(), Lists.emptyList()));
  }
}
