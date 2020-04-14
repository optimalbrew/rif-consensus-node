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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.InMemoryStorageProvider;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.unitrie.NullUniNode;
import org.hyperledger.besu.ethereum.unitrie.UniNode;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class AccountCreationTest {

  private static UniTrieMutableWorldState createEmpty(final WorldStateStorage storage) {
    return new UniTrieMutableWorldState(storage);
  }

  private static UniTrieMutableWorldState createEmpty() {
    final InMemoryStorageProvider provider = new InMemoryStorageProvider();
    return createEmpty(provider.createWorldStateStorage());
  }

  private int size;
  private double alpha;
  private int progress;
  private boolean hits;

  //private boolean aborted;

  private static class PathStats {
    int min;
    int max;
    int seen;
    double avg;
    int nodes;

    void consume(final UniNode node) {
      ++nodes;

      if (node.getLeftChild() == NullUniNode.instance()
          && node.getRightChild() == NullUniNode.instance()) {
        return;
      }

      int len = node.getPath().length;
      min = Math.min(min, len);
      max = Math.max(max, len);

      ++seen;
      avg = (avg * (seen - 1) + len) / seen;
    }

    @Override
    public String toString() {
      return String.format(
          "Total nodes = %d, Branches = %d, Min len = %d, Max len = %d, Path Avg = %.3f",
          nodes, seen, min, max, avg);
    }
  }

  @Before
  public void setup() {
    String accountsProp = System.getProperty("stress.accounts");
    String alphaProp = System.getProperty("stress.alpha");

    size = Strings.isBlank(accountsProp)? 0 : Integer.parseInt(accountsProp.replace("_", ""));
    alpha = Strings.isBlank(alphaProp)? 0 : Double.parseDouble(alphaProp);

    progress = 0;
    hits = false;

    //aborted = false;
  }

  /*
  private void abort(final int secs) {
    System.out.printf("Test timeout after: %d secs\n", secs);
    aborted = true;
  }
  */

  @Test
  public void test_account_creation() {
    Assume.assumeTrue("No # of accounts passed, skipping test", size > 0);

    /*
    // Abort test after 20 minutes
    final int secs = 20*60;
    new Thread(() -> {
      try {
          Thread.sleep(secs*1000);
          abort(secs);
      } catch (InterruptedException e) {
        // Nothing
      }
    }, "timer").start();
    */

    long start = java.lang.System.currentTimeMillis();

    System.out.printf("Inserting %d accounts\n", size);

    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();

    final BytesValue code =
        BytesValue.fromHexString(
            "0x608060405234801561001057600080fd5b506040805180820190915260078082527f546f6b656e204100"
                + "0000000000000000000000000000000000000000000000006020909201918252610055916003916101"
                + "8a565b506040805180820190915260038082527f544b41000000000000000000000000000000000000"
                + "0000000000000000000000602090920191825261009a9160049161018a565b506100b4336509184e72"
                + "a0006401000000006100b9810204565b610225565b600160a060020a03821615156100ce57600080fd"
                + "5b6002546100e890826401000000006106f561017182021704565b600255600160a060020a03821660"
                + "009081526020819052604090205461011b90826401000000006106f561017182021704565b600160a0"
                + "60020a0383166000818152602081815260408083209490945583518581529351929391927fddf252ad"
                + "1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a35050565b"
                + "60008282018381101561018357600080fd5b9392505050565b82805460018160011615610100020316"
                + "6002900490600052602060002090601f016020900481019282601f106101cb57805160ff1916838001"
                + "1785556101f8565b828001600101855582156101f8579182015b828111156101f85782518255916020"
                + "019190600101906101dd565b50610204929150610208565b5090565b61022291905b80821115610204"
                + "576000815560010161020e565b90565b61073a806102346000396000f3006080604052600436106100"
                + "a35763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504"
                + "1663095ea7b381146100a857806318160ddd146100e057806323b872dd146101075780633950935114"
                + "61013157806370a0823114610155578063a457c2d714610176578063a9059cbb1461019a578063b09f"
                + "1266146101be578063d28d885214610248578063dd62ed3e1461025d575b600080fd5b3480156100b4"
                + "57600080fd5b506100cc600160a060020a0360043516602435610284565b6040805191151582525190"
                + "81900360200190f35b3480156100ec57600080fd5b506100f5610302565b6040805191825251908190"
                + "0360200190f35b34801561011357600080fd5b506100cc600160a060020a0360043581169060243516"
                + "604435610308565b34801561013d57600080fd5b506100cc600160a060020a03600435166024356103"
                + "d1565b34801561016157600080fd5b506100f5600160a060020a0360043516610481565b3480156101"
                + "8257600080fd5b506100cc600160a060020a036004351660243561049c565b3480156101a657600080"
                + "fd5b506100cc600160a060020a03600435166024356104e7565b3480156101ca57600080fd5b506101"
                + "d36104fd565b6040805160208082528351818301528351919283929083019185019080838360005b83"
                + "81101561020d5781810151838201526020016101f5565b50505050905090810190601f16801561023a"
                + "5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b"
                + "34801561025457600080fd5b506101d361058b565b34801561026957600080fd5b506100f5600160a0"
                + "60020a03600435811690602435166105e6565b6000600160a060020a038316151561029b57600080fd"
                + "5b336000818152600160209081526040808320600160a060020a038816808552908352928190208690"
                + "55805186815290519293927f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7"
                + "c3b925929181900390910190a350600192915050565b60025490565b600160a060020a038316600090"
                + "815260016020908152604080832033845290915281205461033c908363ffffffff61061116565b6001"
                + "60a060020a038516600090815260016020908152604080832033845290915290205561036b84848461"
                + "0628565b600160a060020a038416600081815260016020908152604080832033808552908352928190"
                + "2054815190815290519293927f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8"
                + "c7c3b925929181900390910190a35060019392505050565b6000600160a060020a03831615156103e8"
                + "57600080fd5b336000908152600160209081526040808320600160a060020a03871684529091529020"
                + "5461041c908363ffffffff6106f516565b336000818152600160209081526040808320600160a06002"
                + "0a0389168085529083529281902085905580519485525191937f8c5be1e5ebec7d5bd14f71427d1e84"
                + "f3dd0314c0f7b2291e5b200ac8c7c3b925929081900390910190a350600192915050565b600160a060"
                + "020a031660009081526020819052604090205490565b6000600160a060020a03831615156104b35760"
                + "0080fd5b336000908152600160209081526040808320600160a060020a038716845290915290205461"
                + "041c908363ffffffff61061116565b60006104f4338484610628565b50600192915050565b60048054"
                + "60408051602060026001851615610100026000190190941693909304601f8101849004840282018401"
                + "90925281815292918301828280156105835780601f1061055857610100808354040283529160200191"
                + "610583565b820191906000526020600020905b81548152906001019060200180831161056657829003"
                + "601f168201915b505050505081565b6003805460408051602060026001851615610100026000190190"
                + "941693909304601f810184900484028201840190925281815292918301828280156105835780601f10"
                + "61055857610100808354040283529160200191610583565b600160a060020a03918216600090815260"
                + "016020908152604080832093909416825291909152205490565b6000808383111561062157600080fd"
                + "5b5050900390565b600160a060020a038216151561063d57600080fd5b600160a060020a0383166000"
                + "90815260208190526040902054610666908263ffffffff61061116565b600160a060020a0380851660"
                + "0090815260208190526040808220939093559084168152205461069b908263ffffffff6106f516565b"
                + "600160a060020a03808416600081815260208181526040918290209490945580518581529051919392"
                + "8716927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9291829003"
                + "0190a3505050565b60008282018381101561070757600080fd5b93925050505600a165627a7a723058"
                + "201ccccf3643bfb06a9c417e8e3da96cf446bae5a3d9fb0da7af9ad966249008e00029");

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
                  account.setCode(code);
                }
              });

      long elapsed = java.lang.System.currentTimeMillis() - start;
      System.out.printf("Elapsed: %s - Committing...\n", fmtMillis(elapsed));
      updater.commit();
      updater.revert();
    }

    /*
    if (aborted) {
      System.out.println("**** Test ABORTED ****");
      return;
    }
    */

    long elapsed = java.lang.System.currentTimeMillis() - start;
    System.out.printf("Elapsed: %s - Commit done...\n", fmtMillis(elapsed));

    //worldState.getTrie().visitAll(__ -> ++nodes);
    PathStats stats = new PathStats();
    worldState.getTrie().visitAll(stats::consume);
    elapsed = java.lang.System.currentTimeMillis() - start;
    //System.out.printf("Elapsed: %s -  Total nodes: %d\n", fmtMillis(elapsed), nodes);
    System.out.printf("Total elapsed: %s\n", fmtMillis(elapsed));
    System.out.printf("Stats: %s\n", stats);

    if (addresses.isEmpty()) {
      addresses.addAll(addresses().limit(maxAddresses).collect(Collectors.toList()));
    }

    performLookups(worldState, addresses);

    /*
    start = System.currentTimeMillis();
    Bytes32 rootHash = worldState.rootHash();
    elapsed = System.currentTimeMillis() - start;
    System.out.printf("Root hash = %s\n", rootHash);
    System.out.printf("Root hash computed in: %s secs\n", fmtMillis(elapsed));
    */
    /*
    UniTrie<?, ?> trie = worldState.getTrie();
    count(trie);
    elapsed = java.lang.System.currentTimeMillis() - start;
    System.out.printf("Elapsed: %s -  Accounts in Unitrie = %d\n", fmtMillis(elapsed), noAccounts);
    assertThat(noAccounts).isEqualTo(size);
    */
  }

  private void performLookups(final WorldState worldState, final List<Address> addresses) {
    AtomicInteger hits = new AtomicInteger(0);
    long start = System.currentTimeMillis();
    addresses.forEach(address -> {
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

  /*
  private void count(final UniTrie<?, ?> unitrie) {
    unitrie.visitAll(
        node -> {
          if (node.getLeftChild() == NullUniNode.instance()
              && node.getRightChild() == NullUniNode.instance()) {
            ++noAccounts;
          }
        });
  }
  */

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
      return Address.extract(Hash.hash(BytesValue.wrap(address)));
    }
  }
}
