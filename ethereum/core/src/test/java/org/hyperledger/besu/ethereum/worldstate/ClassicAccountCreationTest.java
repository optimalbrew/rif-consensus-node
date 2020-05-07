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

import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.InMemoryStorageProvider;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.util.Strings;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

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

  private int size;
  private double alpha;
  private int progress;
  private int nodes;
  private boolean hits;

  @Before
  public void setup() {
    String accountsProp = System.getProperty("stress.accounts");
    String alphaProp = System.getProperty("stress.alpha");

    size = Strings.isBlank(accountsProp) ? 0 : Integer.parseInt(accountsProp.replace("_", ""));
    alpha = Strings.isBlank(alphaProp) ? 0 : Double.parseDouble(alphaProp);

    progress = 0;
    nodes = 0;
    hits = false;
  }

  @Test
  public void test_account_creation() {
    Assume.assumeTrue("No # of accounts passed, skipping test", size > 0);

    long start = java.lang.System.currentTimeMillis();

    System.out.printf("Inserting %d accounts\n", size);

    final DefaultMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();

    int maxAddresses = 1_000_000;
    List<Address> addresses = new ArrayList<>(maxAddresses);

    int batchCount = 50;
    for (int batch = 0; batch < batchCount; batch++) {
      System.out.printf("Batch: %d\n", batch);

      addresses()
          .limit(size / batchCount)
          .forEach(
              address -> {
                ++progress;
                if (progress % 1000 == 0) {
                  System.out.printf("Progress: %d\n", progress);
                }

                if (hits && addresses.size() < maxAddresses) {
                  addresses.add(address);
                }

                MutableAccount account = updater.createAccount(address).getMutable();
                account.setBalance(Wei.of(100000));

                if (Math.random() < alpha) {
                  account.setCode(AccountCode.SHORT_CODE);
                }
              });

      long elapsed = java.lang.System.currentTimeMillis() - start;
      System.out.printf("Elapsed: %s - Committing...\n", fmtMillis(elapsed));
      updater.commit();
      updater.revert();

      Bytes32 rootHash = worldState.rootHash();
      System.out.printf("Root hash = %s\n", rootHash);
    }

    long elapsed = java.lang.System.currentTimeMillis() - start;
    System.out.printf("Elapsed: %s - Commit done...\n", fmtMillis(elapsed));

    worldState.getAccountStateTrie().visitAll(__ -> ++nodes);
    elapsed = java.lang.System.currentTimeMillis() - start;
    System.out.printf("Elapsed: %s -  Total nodes: %d\n", fmtMillis(elapsed), nodes);

    if (addresses.isEmpty()) {
      addresses.addAll(addresses().limit(maxAddresses).collect(Collectors.toList()));
    }

    performLookups(worldState, addresses);
  }

  private void performLookups(final WorldState worldState, final List<Address> addresses) {
    AtomicInteger hits = new AtomicInteger(0);
    long start = System.currentTimeMillis();
    addresses.forEach(
        address -> {
          if (worldState.get(address) != null) {
            hits.incrementAndGet();
          }
        });
    long elapsed = System.currentTimeMillis() - start;
    System.out.printf(
        "Looked up for %d accounts, found %d, elapsed = %s\n",
        addresses.size(), hits.intValue(), fmtMillis(elapsed));
  }

  private String fmtMillis(final long milliSecs) {
    return String.format("%.3f", milliSecs / 1_000.0d);
  }

  private Stream<Address> addresses() {
    return Stream.generate(() -> Account.create().getAddress());
  }

  private static class Account {
    static Random random = new Random();

    byte[] address;

    Account(final byte[] address) {
      this.address = address;
    }

    static Account create() {
      byte[] b = new byte[20];
      random.nextBytes(b);
      return new Account(b);
    }

    Address getAddress() {
      return Address.extract(Hash.hash(Bytes.wrap(address)));
    }
  }
}
