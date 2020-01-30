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
package org.hyperledger.besu.chainimport.bulk;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.common.io.Resources;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.hyperledger.besu.chainimport.JsonBlockImporter;
import org.hyperledger.besu.config.GenesisConfigFile;
import org.hyperledger.besu.controller.BesuController;
import org.hyperledger.besu.controller.GasLimitCalculator;
import org.hyperledger.besu.crypto.SECP256K1.KeyPair;
import org.hyperledger.besu.ethereum.core.InMemoryStorageProvider;
import org.hyperledger.besu.ethereum.core.MiningParametersTestBuilder;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.eth.EthProtocolConfiguration;
import org.hyperledger.besu.ethereum.eth.sync.SynchronizerConfiguration;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.hyperledger.besu.ethereum.merkleutils.ClassicMerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleStorageMode;
import org.hyperledger.besu.ethereum.merkleutils.UniTrieMerkleAwareProvider;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.testutil.TestClock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
@Fork(value = 2, jvmArgsAppend = {"-server", "-disablesystemassertions"})
@OutputTimeUnit(TimeUnit.SECONDS)
public class BulkJsonImportBenchmark {

  private Path temporaryFolder;
  private MerkleAwareProvider merkleAwareProvider;
  private GenesisConfigFile genesisConfigFile;
  private String jsonData;
  private BesuController<?> controller;
  private JsonBlockImporter<?> importer;

  @Param({"CLASSIC", "UNITRIE"})
  MerkleStorageMode merkleStorageMode;

  @Setup(Level.Invocation)
  public void setUp() throws IOException {
    temporaryFolder = Files.createTempDir().toPath();
    merkleAwareProvider = merkleAwareProvider(merkleStorageMode);
    genesisConfigFile = genesisConfigFile();
    jsonData = getFileContents("blocks.json");
    controller = createController(merkleAwareProvider, genesisConfigFile);
    importer = new JsonBlockImporter<>(controller);
  }

  @TearDown(Level.Invocation)
  public void tearDown() throws IOException {
    MoreFiles.deleteRecursively(temporaryFolder, RecursiveDeleteOption.ALLOW_INSECURE);
  }

  @Benchmark
  public void importChain() throws IOException {
    importer.importChain(jsonData);
  }

  private String getFileContents(final String filename) throws IOException {
    final String filePath = "/bulk/" + filename;
    final URL fileURL = this.getClass().getResource(filePath);
    return Resources.toString(fileURL, UTF_8);
  }

  private GenesisConfigFile genesisConfigFile() throws IOException {
    return GenesisConfigFile.fromConfig(getFileContents("genesis.json"));
  }

  private BesuController<?> createController(
      final MerkleAwareProvider merkleAwareProvider, final GenesisConfigFile genesisConfigFile) {

    final Path dataDir = temporaryFolder;
    return new BesuController.Builder()
        .fromGenesisConfig(genesisConfigFile)
        .merkleAwareProvider(merkleAwareProvider)
        .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
        .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
        .storageProvider(new InMemoryStorageProvider())
        .networkId(BigInteger.valueOf(10))
        .miningParameters(
            new MiningParametersTestBuilder()
                .minTransactionGasPrice(Wei.ZERO)
                .enabled(false)
                .build())
        .nodeKeys(KeyPair.generate())
        .metricsSystem(new NoOpMetricsSystem())
        .privacyParameters(PrivacyParameters.DEFAULT)
        .dataDirectory(dataDir)
        .clock(TestClock.fixed())
        .transactionPoolConfiguration(TransactionPoolConfiguration.builder().build())
        .targetGasLimit(GasLimitCalculator.DEFAULT)
        .build();
  }

  private MerkleAwareProvider merkleAwareProvider(final MerkleStorageMode merkleStorageMode) {
    switch(merkleStorageMode) {
      case UNITRIE:
        return new UniTrieMerkleAwareProvider();
      case CLASSIC:
      default:
        return new ClassicMerkleAwareProvider();
    }
  }
}
