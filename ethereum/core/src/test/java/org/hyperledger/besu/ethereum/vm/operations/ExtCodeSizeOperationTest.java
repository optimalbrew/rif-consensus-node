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
package org.hyperledger.besu.ethereum.vm.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.core.InMemoryStorageProvider.createInMemoryUniTrieWorldStateArchive;
import static org.hyperledger.besu.ethereum.core.InMemoryStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.Mockito.mock;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.AddressHelpers;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderTestFixture;
import org.hyperledger.besu.ethereum.core.Gas;
import org.hyperledger.besu.ethereum.core.MessageFrameTestFixture;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.mainnet.IstanbulGasCalculator;
import org.hyperledger.besu.ethereum.vm.MessageFrame;
import org.hyperledger.besu.ethereum.vm.Words;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.uint.UInt256;
import org.hyperledger.besu.util.uint.UInt256Bytes;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExtCodeSizeOperationTest {

  @Parameters
  public static Iterable<WorldStateArchive> data() {
    return Arrays.asList(defaultWorldStateArchive(), uniTrieWorldStateArchive());
  }

  private static WorldStateArchive defaultWorldStateArchive() {
    return createInMemoryWorldStateArchive();
  }

  private static WorldStateArchive uniTrieWorldStateArchive() {
    return createInMemoryUniTrieWorldStateArchive();
  }

  private static final Address REQUESTED_ADDRESS = AddressHelpers.ofValue(22222222);

  private final Blockchain blockchain = mock(Blockchain.class);

  private final ExtCodeSizeOperation operation =
      new ExtCodeSizeOperation(new IstanbulGasCalculator());

  private final WorldUpdater worldStateUpdater;

  public ExtCodeSizeOperationTest(final WorldStateArchive worldStateArchive) {
    this.worldStateUpdater = worldStateArchive.getMutable().updater();
  }

  @Test
  public void shouldCharge700Gas() {
    assertThat(operation.cost(createMessageFrame(REQUESTED_ADDRESS))).isEqualTo(Gas.of(700));
  }

  @Test
  public void shouldReturnZero_whenAccountDoesNotExist() {
    final Bytes32 result = executeOperation(REQUESTED_ADDRESS);
    assertThat(result).isEqualTo(Bytes32.ZERO);
  }

  @Test
  public void shouldReturnZero_whenAccountExistsButDoesNotHaveCode() {
    worldStateUpdater.getOrCreate(REQUESTED_ADDRESS).getMutable().setBalance(Wei.of(1));
    assertThat(executeOperation(REQUESTED_ADDRESS)).isEqualTo(Bytes32.ZERO);
  }

  @Test
  public void shouldReturnZero_whenAccountExistsButIsEmpty() {
    worldStateUpdater.getOrCreate(REQUESTED_ADDRESS);
    assertThat(executeOperation(REQUESTED_ADDRESS)).isEqualTo(Bytes32.ZERO);
  }

  @Test
  public void shouldReturnZero_whenPrecompiledContractHasNoBalance() {
    assertThat(executeOperation(Address.ECREC)).isEqualTo(Bytes32.ZERO);
  }

  @Test
  public void shouldReturnZero_whenPrecompileHasBalance() {
    worldStateUpdater.getOrCreate(Address.ECREC).getMutable().setBalance(Wei.of(10));
    assertThat(executeOperation(Address.ECREC)).isEqualTo(Bytes32.ZERO);
  }

  @Test
  public void shouldGetSizeOfAccountCode_whenCodeIsPresent() {
    final BytesValue code = BytesValue.fromHexString("0xabcdef");
    final MutableAccount account = worldStateUpdater.getOrCreate(REQUESTED_ADDRESS).getMutable();
    account.setCode(code);
    account.setVersion(Account.DEFAULT_VERSION);
    assertThat(executeOperation(REQUESTED_ADDRESS)).isEqualTo(UInt256Bytes.of(code.size()));
  }

  @Test
  public void shouldZeroOutLeftMostBitsToGetAddress() {
    // Address should be equivalent mod 2**160
    final BytesValue code = BytesValue.fromHexString("0xabcdef");
    final MutableAccount account = worldStateUpdater.getOrCreate(REQUESTED_ADDRESS).getMutable();
    account.setCode(code);
    account.setVersion(Account.DEFAULT_VERSION);
    final Bytes32 value =
        Words.fromAddress(REQUESTED_ADDRESS)
            .asUInt256()
            .plus(UInt256.of(2).pow(UInt256.of(160)))
            .getBytes();
    final MessageFrame frame = createMessageFrame(value);
    operation.execute(frame);
    assertThat(frame.getStackItem(0)).isEqualTo(UInt256Bytes.of(code.size()));
  }

  private Bytes32 executeOperation(final Address requestedAddress) {
    final MessageFrame frame = createMessageFrame(requestedAddress);
    operation.execute(frame);
    return frame.getStackItem(0);
  }

  private MessageFrame createMessageFrame(final Address requestedAddress) {
    final Bytes32 stackItem = Words.fromAddress(requestedAddress);
    return createMessageFrame(stackItem);
  }

  private MessageFrame createMessageFrame(final Bytes32 stackItem) {
    final BlockHeader blockHeader = new BlockHeaderTestFixture().buildHeader();
    final MessageFrame frame =
        new MessageFrameTestFixture()
            .worldState(worldStateUpdater)
            .blockHeader(blockHeader)
            .blockchain(blockchain)
            .build();

    frame.pushStackItem(stackItem);
    return frame;
  }
}
