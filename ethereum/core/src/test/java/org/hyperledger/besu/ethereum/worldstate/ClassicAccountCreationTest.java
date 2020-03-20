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

import java.util.stream.Stream;
import org.hyperledger.besu.crypto.SECP256K1.KeyPair;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.InMemoryStorageProvider;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ClassicAccountCreationTest {

  private static DefaultMutableWorldState createEmpty(
      final WorldStateStorage storage, final WorldStatePreimageStorage preImageStorage) {
    return new DefaultMutableWorldState(storage, preImageStorage);
  }

  private static DefaultMutableWorldState createEmpty() {
    final InMemoryStorageProvider provider = new InMemoryStorageProvider();
    return createEmpty(
        provider.createWorldStateStorage(), provider.createWorldStatePreimageStorage());
  }

  @Parameters
  public static Object[] data() {
    return new Object[] {2_937_500};
  }

  @Parameter
  public int size;

  private int progress;

  @Before
  public void setup() {
    progress = 0;
  }

  @Test
  public void test_account_creation() {
    System.out.printf("Inserting %d accounts\n", size);

    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();

    addresses()
        .limit(size)
        .forEach(address -> {
          ++progress;
          if (progress % 1000 == 0) {
            System.out.printf("Progress: %d\n", progress);
          }
          updater.createAccount(address).getMutable().setBalance(Wei.of(100000));
        });

    updater.commit();
    worldState.persist();

    long count = worldState.streamAccounts(Bytes32.ZERO, Integer.MAX_VALUE).count();
    assertThat(count).isEqualTo(size);
  }

  private Stream<Address> addresses() {
    return Stream.generate(() -> ClassicAccountCreationTest.Account.create().getAddress());
  }

  private static class Account {
    KeyPair keyPair;

    Account(final KeyPair keyPair) {
      this.keyPair = keyPair;
    }

    static ClassicAccountCreationTest.Account create() {
      return new Account(KeyPair.generate());
    }

    Address getAddress() {
      return Address.extract(Hash.hash(keyPair.getPublicKey().getEncodedBytes()));
    }
  }
}
