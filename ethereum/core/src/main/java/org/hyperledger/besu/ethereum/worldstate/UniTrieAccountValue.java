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

import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

/** Represents the raw values associated with an account in the world state UniTrie. */
public class UniTrieAccountValue {

  private final long nonce;
  private final Wei balance;
  private final int version;

  private UniTrieAccountValue(final long nonce, final Wei balance) {
    this(nonce, balance, Account.DEFAULT_VERSION);
  }

  public UniTrieAccountValue(final long nonce, final Wei balance, final int version) {
    this.nonce = nonce;
    this.balance = balance;
    this.version = version;
  }

  /**
   * The account nonce, that is the number of transactions sent from that account.
   *
   * @return the account nonce.
   */
  public long getNonce() {
    return nonce;
  }

  /**
   * The available balance of that account.
   *
   * @return the balance, in Wei, of the account.
   */
  public Wei getBalance() {
    return balance;
  }

  /**
   * The version of the EVM bytecode associated with this account.
   *
   * @return the version of the account code.
   */
  public int getVersion() {
    return version;
  }

  public void writeTo(final RLPOutput out) {
    out.startList();

    out.writeLongScalar(nonce);
    out.writeUInt256Scalar(balance);

    if (version != Account.DEFAULT_VERSION) {
      // version of zero is never written out.
      out.writeIntScalar(version);
    }

    out.endList();
  }

  public static UniTrieAccountValue readFrom(final RLPInput in) {
    in.enterList();

    final long nonce = in.readLongScalar();
    final Wei balance = Wei.of(in.readUInt256Scalar());
    final int version;
    if (!in.isEndOfCurrentList()) {
      version = in.readIntScalar();
    } else {
      version = Account.DEFAULT_VERSION;
    }

    in.leaveList();

    return new UniTrieAccountValue(nonce, balance, version);
  }
}
