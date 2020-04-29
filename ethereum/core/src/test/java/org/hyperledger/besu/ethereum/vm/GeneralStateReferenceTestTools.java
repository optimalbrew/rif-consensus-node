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
 */
package org.hyperledger.besu.ethereum.vm;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.Log;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.mainnet.TransactionProcessor;
import org.hyperledger.besu.ethereum.mainnet.TransactionValidationParams;
import org.hyperledger.besu.ethereum.merkleutils.ClassicMerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.UniTrieMerkleAwareProvider;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.testutil.JsonTestParameters;
import org.hyperledger.besu.testutil.JsonTestParameters.Generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GeneralStateReferenceTestTools {
  private static final ReferenceTestProtocolSchedules REFERENCE_TEST_PROTOCOL_SCHEDULES =
      ReferenceTestProtocolSchedules.create();
  private static final List<String> SPECS_PRIOR_TO_DELETING_EMPTY_ACCOUNTS =
      Arrays.asList("Frontier", "Homestead", "EIP150");

  private static TransactionProcessor transactionProcessor(final String name) {
    return REFERENCE_TEST_PROTOCOL_SCHEDULES
        .getByName(name)
        .getByBlockNumber(0)
        .getTransactionProcessor();
  }

  private static final List<String> EIPS_TO_RUN;

  static {
    final String eips =
        System.getProperty(
            "test.ethereum.state.eips",
            "Frontier,Homestead,EIP150,EIP158,Byzantium,Constantinople,ConstantinopleFix,Istanbul");
    EIPS_TO_RUN = Arrays.asList(eips.split(","));
  }

  private static final JsonTestParameters<?, ?> classicParams =
      JsonTestParameters.create(GeneralStateTestCaseSpec.class, GeneralStateTestCaseEipSpec.class)
          .generator(generator(new ClassicMerkleAwareProvider()));

  private static final JsonTestParameters<?, ?> uniTrieParams =
      JsonTestParameters.create(
              UniTrieGeneralStateTestCaseSpec.class, GeneralStateTestCaseEipSpec.class)
          .generator(generator(new UniTrieMerkleAwareProvider()));

  private static void setupParams(final JsonTestParameters<?, ?> params) {
    if (EIPS_TO_RUN.isEmpty()) {
      params.blacklistAll();
    }

    // Known incorrect test.
    params.blacklist(
        "RevertPrecompiledTouch(_storage)?-(EIP158|Byzantium|Constantinople|ConstantinopleFix)");

    // Gas integer value is too large to construct a valid transaction.
    params.blacklist("OverflowGasRequire");

    // Consumes a huge amount of memory
    params.blacklist("static_Call1MB1024Calldepth-\\w");
  }

  static {
    setupParams(classicParams);
    setupParams(uniTrieParams);
  }

  /**
   * Generate either classic or UniTrie {@link GeneralStateTestCaseEipSpec} instances.
   *
   * @param merkleAwareProvider provider for the corresponding Merkle storage mode
   * @param <S> type of general test case spec to use as input
   * @return generator providing classic or UniTrie tests
   */
  private static <S extends AbstractGeneralStateTestCaseSpec>
      Generator<S, GeneralStateTestCaseEipSpec> generator(
          final MerkleAwareProvider merkleAwareProvider) {

    return (testName, stateSpec, collector) -> {
      final String prefix = merkleAwareProvider.toString().toLowerCase() + "-" + testName + "-";
      for (final Map.Entry<String, List<GeneralStateTestCaseEipSpec>> entry :
          stateSpec.finalStateSpecs().entrySet()) {
        final String eip = entry.getKey();
        final boolean runTest = EIPS_TO_RUN.contains(eip);
        final List<GeneralStateTestCaseEipSpec> eipSpecs = entry.getValue();
        if (eipSpecs.size() == 1) {
          collector.add(prefix + eip, eipSpecs.get(0), runTest);
        } else {
          for (int i = 0; i < eipSpecs.size(); i++) {
            collector.add(prefix + eip + '[' + i + ']', eipSpecs.get(i), runTest);
          }
        }
      }
    };
  }

  public static Collection<Object[]> generateTestParametersForConfig(final String[] filePath) {
    String exclude = System.getProperty("test.general.state.exclude");
    if (exclude != null && exclude.equalsIgnoreCase("unitrie")) {
      return classicParams.generate(filePath);
    }
    if (exclude != null && exclude.equalsIgnoreCase("classic")) {
      return uniTrieParams.generate(filePath);
    }
    Collection<Object[]> specs = classicParams.generate(filePath);
    specs.addAll(uniTrieParams.generate(filePath));
    return specs;
  }

  public static void executeTest(final GeneralStateTestCaseEipSpec spec) {
    final BlockHeader blockHeader = spec.blockHeader();
    final Transaction transaction = spec.transaction();

    final MutableWorldState worldState = spec.getRuntimeWorldState();

    // Several of the GeneralStateTests check if the transaction could potentially
    // consume more gas than is left for the block it's attempted to be included in.
    // This check is performed within the `BlockImporter` rather than inside the
    // `TransactionProcessor`, so these tests are skipped.
    if (transaction.getGasLimit() > blockHeader.getGasLimit() - blockHeader.getGasUsed()) {
      return;
    }

    final TransactionProcessor processor = transactionProcessor(spec.eip());
    final WorldUpdater worldStateUpdater = worldState.updater();
    final TestBlockchain blockchain = new TestBlockchain(blockHeader.getNumber());
    final TransactionProcessor.Result result =
        processor.processTransaction(
            blockchain,
            worldStateUpdater,
            blockHeader,
            transaction,
            blockHeader.getCoinbase(),
            new BlockHashLookup(blockHeader, blockchain),
            false,
            TransactionValidationParams.processingBlock());
    final Account coinbase = worldStateUpdater.getOrCreate(spec.blockHeader().getCoinbase());
    if (coinbase != null && coinbase.isEmpty() && shouldClearEmptyAccounts(spec.eip())) {
      worldStateUpdater.deleteAccount(coinbase.getAddress());
    }
    worldStateUpdater.commit();

    // If the EIP test spec allows it, check the world state root hash.
    // We don't want to do the check when using UniTries, which encode
    // differently ans thus give a distinct hash.
    if (spec.shouldCheckRootHash()) {
      final Hash expectedRootHash = spec.expectedRootHash();
      assertThat(worldState.rootHash())
          .withFailMessage("Unexpected world state root hash; computed state: %s", worldState)
          .isEqualByComparingTo(expectedRootHash);
    }

    // Check the logs.
    final Hash expectedLogsHash = spec.expectedLogsHash();
    final List<Log> logs = result.getLogs();
    assertThat(Hash.hash(RLP.encode(out -> out.writeList(logs, Log::writeTo))))
        .withFailMessage("Unmatched logs hash. Generated logs: %s", logs)
        .isEqualTo(expectedLogsHash);
  }

  private static boolean shouldClearEmptyAccounts(final String eip) {
    return !SPECS_PRIOR_TO_DELETING_EMPTY_ACCOUNTS.contains(eip);
  }
}
