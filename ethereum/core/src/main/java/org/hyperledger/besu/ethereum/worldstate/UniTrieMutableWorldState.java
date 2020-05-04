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

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Stream;
import org.hyperledger.besu.ethereum.core.AbstractWorldUpdater;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.AccountStorageEntry;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPException;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.unitrie.StoredUniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrieKeyMapper;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

/**
 * Mutable world state witnessed by Unitries.
 *
 * @author ppedemon
 */
public class UniTrieMutableWorldState implements MutableWorldState {

  private final WorldStateStorage worldStateStorage;
  private final UniTrie<Bytes, Bytes> trie;

  private final Map<Address, Bytes> updatedAccountCode = new HashMap<>();
  private final UniTrieKeyMapper keyMapper = new UniTrieKeyMapper();

  public UniTrieMutableWorldState(final WorldStateStorage storage) {
    this(UniTrie.NULL_UNINODE_HASH, storage);
  }

  public UniTrieMutableWorldState(
      final Bytes32 rootHash, final WorldStateStorage worldStateStorage) {
    this.worldStateStorage = worldStateStorage;
    this.trie = initTrie(rootHash);
  }

  public UniTrieMutableWorldState(final WorldState worldState) {
    if (!(worldState instanceof UniTrieMutableWorldState)) {
      throw new UnsupportedOperationException();
    }

    final UniTrieMutableWorldState other = (UniTrieMutableWorldState) worldState;
    this.worldStateStorage = other.worldStateStorage;
    this.trie = initTrie(other.trie.getRootHash());
  }

  private UniTrie<Bytes, Bytes> initTrie(final Bytes32 rootHash) {
    return new StoredUniTrie<>(
        worldStateStorage::getAccountStateTrieNode, rootHash, b -> b, b -> b);
  }

  public UniTrie<Bytes, Bytes> getTrie() {
    return trie;
  }

  @Override
  public Hash rootHash() {
    return Hash.wrap(trie.getRootHash());
  }

  @Override
  public MutableWorldState copy() {
    return new UniTrieMutableWorldState(rootHash(), worldStateStorage);
  }

  @Override
  public void persist() {
    final WorldStateStorage.Updater stateUpdater = worldStateStorage.updater();
    trie.commit(stateUpdater::putAccountStateTrieNode, stateUpdater::rawPut);
    updatedAccountCode.clear();
    stateUpdater.commit();
  }

  @Override
  public WorldUpdater updater() {
    return new Updater(this);
  }

  @Override
  public Stream<StreamableAccount> streamAccounts(final Bytes32 startKeyHash, final int limit) {
    throw new UnsupportedOperationException(
        "Unitries don't associate account entries to their "
            + "hashes, so they don't support iterating from a given account entry hash");
  }

  @Override
  public Account get(final Address address) {
    final Bytes mappedKey = keyMapper.getAccountKey(address);
    return trie.get(mappedKey)
        .map(bytes -> deserializeAccount(address, bytes))
        .orElse(null);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rootHash());
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof UniTrieMutableWorldState)) {
      return false;
    }

    final UniTrieMutableWorldState that = (UniTrieMutableWorldState) other;
    return this.rootHash().equals(that.rootHash());
  }

  @VisibleForTesting
  UniTrie<Bytes, Bytes> getUniTrie() {
    return trie;
  }

  private WorldStateAccount deserializeAccount(final Address address, final Bytes encoded)
      throws RLPException {

    final RLPInput in = RLP.input(encoded);
    final UniTrieAccountValue accountValue = UniTrieAccountValue.readFrom(in);
    return new WorldStateAccount(address, accountValue);
  }

  private static Bytes serializeAccount(
      final long nonce, final Wei balance, final int version) {
    final UniTrieAccountValue accountValue = new UniTrieAccountValue(nonce, balance, version);
    return RLP.encode(accountValue::writeTo);
  }

  /**
   * Immutable account class representing an individual account as stored in the world state's
   * underlying Unitrie.
   */
  protected class WorldStateAccount implements Account {

    private final Address address;
    private final UniTrieAccountValue accountValue;

    private WorldStateAccount(final Address address, final UniTrieAccountValue accountValue) {
      this.address = address;
      this.accountValue = accountValue;
    }

    @Override
    public Address getAddress() {
      return address;
    }

    @Override
    public Hash getAddressHash() {
      // NB: this call is only used to support retesteth debug_AccountRange method,
      // which the UniTrie doesn't support.
      throw new UnsupportedOperationException("Unitrie stored accounts can't return address hash");
    }

    @Override
    public long getNonce() {
      return accountValue.getNonce();
    }

    @Override
    public Wei getBalance() {
      return accountValue.getBalance();
    }

    @Override
    public Bytes getCode() {
      // Attempt to read from updated code cache first
      final Bytes updatedCode = updatedAccountCode.get(address);
      if (updatedCode != null) {
        return updatedCode;
      }

      // The account has code, we must retrieve it from the Unitrie. Since Unitries
      // don't associate code entries to the code hash, the lookup key can't be the
      // code hash. The key must come from the key mapper.
      Bytes mappedKey = keyMapper.getAccountCodeKey(address);
      return trie.get(mappedKey).orElse(Bytes.EMPTY);
    }

    @Override
    public boolean hasCode() {
      return !getCode().isEmpty();
    }

    @Override
    public Hash getCodeHash() {
      if (updatedAccountCode.containsKey(address)) {
        return Hash.hash(updatedAccountCode.get(address));
      }

      Bytes mappedKey = keyMapper.getAccountCodeKey(address);
      return trie.getValueHash(mappedKey).map(Hash::wrap).orElse(Hash.EMPTY);
    }

    @Override
    public Bytes32 getCodeSize() {
      if (updatedAccountCode.containsKey(address)) {
        return UInt256.valueOf(updatedAccountCode.get(address).size()).toBytes();
      }

      Bytes mappedKey = keyMapper.getAccountCodeKey(address);
      return trie.getValueLength(mappedKey).map(n -> UInt256.valueOf(n).toBytes()).orElse(Bytes32.ZERO);
    }

    @Override
    public int getVersion() {
      return accountValue.getVersion();
    }

    @Override
    public UInt256 getStorageValue(final UInt256 key) {
      // UniTries don't associate storage entries to their hashes. So the lookup
      // key for a storage entry must come from the key mapper.
      Bytes mappedKey = keyMapper.getAccountStorageKey(address, key);
      return trie.get(mappedKey).map(this::convertToUInt256).orElse(UInt256.ZERO);
    }

    @Override
    public UInt256 getOriginalStorageValue(final UInt256 key) {
      return getStorageValue(key);
    }

    @Override
    public NavigableMap<Bytes32, AccountStorageEntry> storageEntriesFrom(
        final Bytes32 startKeyHash, final int limit) {
      throw new UnsupportedOperationException(
          "Unitries don't associate storage entries to their "
              + "hashes, so they don't support iterating from a given storage entry hash");
    }

    private UInt256 convertToUInt256(final Bytes value) {
      // TODO: have an optimized method to decode a single scalar since it's used pretty often
      final RLPInput in = RLP.input(value);
      return in.readUInt256Scalar();
    }

    @Override
    public String toString() {
      return "AccountState" + "{"
          + "address=" + getAddress() + ", "
          + "nonce=" + getNonce() + ", "
          + "balance=" + getBalance() + ", "
          + "version=" + getVersion()
          + "}";
    }
  }

  /** Mutable world updater. */
  protected static class Updater
      extends AbstractWorldUpdater<
          UniTrieMutableWorldState, UniTrieMutableWorldState.WorldStateAccount> {

    protected Updater(final UniTrieMutableWorldState world) {
      super(world);
    }


    @Override
    public Collection<Address> getDeletedAccountAddresses() {
      return new ArrayList<>(deletedAccounts());
    }

    @Override
    public Collection<UpdateTrackingAccount<? extends Account>> getTouchedAccounts() {
      return new ArrayList<>(updatedAccounts());
    }

    @Override
    protected UniTrieMutableWorldState.WorldStateAccount getForMutation(final Address address) {
      final UniTrieMutableWorldState wrapped = wrappedWorldView();
      final Bytes mappedKey = wrapped.keyMapper.getAccountKey(address);
      return wrapped
          .trie
          .get(mappedKey)
          .map(bytes -> wrapped.deserializeAccount(address, bytes))
          .orElse(null);
    }


    @Override
    public void revert() {
      deletedAccounts().clear();
      updatedAccounts().clear();
    }

    @Override
    public void commit() {
      final UniTrieMutableWorldState wrapped = wrappedWorldView();

      for (final Address address : deletedAccounts()) {
        final Bytes accountKey = wrapped.keyMapper.getAccountKey(address);
        wrapped.trie.removeRecursive(accountKey);
        wrapped.updatedAccountCode.remove(address);
      }

      for (final UpdateTrackingAccount<UniTrieMutableWorldState.WorldStateAccount> updated :
          updatedAccounts()) {

        Address address = updated.getAddress();

        // Persist updated code if necessary
        if (updated.codeWasUpdated()) {
          Bytes updatedCode = updated.getCode();
          wrapped.updatedAccountCode.put(address, updatedCode);
          if (updatedCode.isEmpty()) {
            wrapped.trie.remove(wrapped.keyMapper.getAccountCodeKey(address));
          } else {
            wrapped.trie.put(wrapped.keyMapper.getAccountCodeKey(address), updatedCode);
          }
        }

        // Persist account storage
        Bytes storageRootPrefixKey = wrapped.keyMapper.getAccountStoragePrefixKey(address);

        if (updated.getStorageWasCleared()) {
          wrapped.trie.removeRecursive(storageRootPrefixKey);
        }

        final Map<UInt256, UInt256> updatedStorage = updated.getUpdatedStorage();
        if (!updatedStorage.isEmpty()) {
          wrapped.trie.put(storageRootPrefixKey, Bytes.of(0));
          for (final Map.Entry<UInt256, UInt256> entry : updatedStorage.entrySet()) {
            final UInt256 value = entry.getValue();
            final Bytes storageKey =
                wrapped.keyMapper.getAccountStorageKey(address, entry.getKey());
            if (value.isZero()) {
              wrapped.trie.remove(storageKey);
            } else {
              wrapped.trie.put(
                  storageKey, RLP.encode(out -> out.writeUInt256Scalar(entry.getValue())));
            }
          }

          if (wrapped.trie.isLeaf(storageRootPrefixKey)) {
            wrapped.trie.remove(storageRootPrefixKey);
          }
        }

        // Finally save the new/updated account
        final Bytes account =
            serializeAccount(updated.getNonce(), updated.getBalance(), updated.getVersion());

        Bytes accountKey = wrapped.keyMapper.getAccountKey(address);
        wrapped.trie.put(accountKey, account);
      }
    }
  }
}
