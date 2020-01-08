/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
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
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.uint.UInt256;
import org.hyperledger.besu.util.uint.UInt256Bytes;

/**
 * Mutable world state witnessed by Unitries.
 *
 * @author ppedemon
 */
public class UniTrieMutableWorldState implements MutableWorldState {

  private final WorldStateStorage worldStateStorage;
  private final UniTrie<BytesValue, BytesValue> trie;

  private final Map<Address, BytesValue> updatedAccountCode = new HashMap<>();
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

  private UniTrie<BytesValue, BytesValue> initTrie(final Bytes32 rootHash) {
    return new StoredUniTrie<>(
        worldStateStorage::getAccountStateTrieNode, rootHash, b -> b, b -> b);
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
    throw new UnsupportedOperationException("Unitries don't associate account entries to their "
        + "hashes, so they don't support iterating from a given account entry hash");
  }

  @Override
  public Account get(final Address address) {
    final BytesValue mappedKey = keyMapper.getAccountKey(address);
    return trie
        .get(mappedKey)
        .map(bytes -> deserializeAccount(address, Hash.hash(address), bytes))
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
  UniTrie<BytesValue, BytesValue> getUniTrie() {
    return trie;
  }

  private WorldStateAccount deserializeAccount(
      final Address address, final Hash addressHash, final BytesValue encoded) throws RLPException {

    final RLPInput in = RLP.input(encoded);
    final StateTrieAccountValue accountValue = StateTrieAccountValue.readFrom(in);
    return new WorldStateAccount(address, addressHash, accountValue);
  }

  private static BytesValue serializeAccount(
      final long nonce,
      final Wei balance,
      final Hash storageRoot,
      final Hash codeHash,
      final int version) {
    final StateTrieAccountValue accountValue =
        new StateTrieAccountValue(nonce, balance, storageRoot, codeHash, version);
    return RLP.encode(accountValue::writeTo);
  }

  /**
   * Immutable account class representing an individual account as stored
   * in the world state's underlying Unitrie.
   */
  protected class WorldStateAccount implements Account {

    private final Address address;
    private final Hash addressHash;
    final StateTrieAccountValue accountValue;

    private WorldStateAccount(
        final Address address, final Hash addressHash, final StateTrieAccountValue accountValue) {

      this.address = address;
      this.addressHash = addressHash;
      this.accountValue = accountValue;
    }

    @Override
    public Address getAddress() {
      return address;
    }

    @Override
    public Hash getAddressHash() {
      return addressHash;
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
    public BytesValue getCode() {
      // Attempt to read from updated code cache first
      final BytesValue updatedCode = updatedAccountCode.get(address);
      if (updatedCode != null) {
        return updatedCode;
      }

      // Cache miss. Let's see if code hash is empty. In that case the account has no code.
      final Hash codeHash = getCodeHash();
      if (codeHash.equals(Hash.EMPTY)) {
        return BytesValue.EMPTY;
      }

      // The account has code, we must retrieve it from the Unitrie. Since Unitries
      // don't associate code entries to the code hash, the lookup key can't be the
      // code hash. The key must come from the key mapper.
      BytesValue mappedKey = keyMapper.getAccountCodeKey(address);
      return trie.get(mappedKey).orElse(BytesValue.EMPTY);
    }

    @Override
    public boolean hasCode() {
      return !getCode().isEmpty();
    }

    @Override
    public Hash getCodeHash() {
      return accountValue.getCodeHash();
    }

    @Override
    public Bytes32 getCodeSize() {
      final BytesValue updatedCode = updatedAccountCode.get(address);
      if (updatedCode != null) {
        return UInt256Bytes.of(updatedCode.size());
      }

      final Hash codeHash = getCodeHash();
      if (codeHash.equals(Hash.EMPTY)) {
        return Bytes32.ZERO;
      }

      BytesValue mappedKey = keyMapper.getAccountCodeKey(address);
      return trie.getValueLength(mappedKey).map(UInt256Bytes::of).orElse(Bytes32.ZERO);
    }

    @Override
    public int getVersion() {
      return accountValue.getVersion();
    }

    @Override
    public UInt256 getStorageValue(final UInt256 key) {
      // UniTries don't associate storage entries to their hashes. So the lookup
      // key for a storage entry must come from the key mapper.
      BytesValue mappedKey = keyMapper.getAccountStorageKey(address, key);
      return trie.get(mappedKey).map(this::convertToUInt256).orElse(UInt256.ZERO);
    }

    @Override
    public UInt256 getOriginalStorageValue(final UInt256 key) {
      return getStorageValue(key);
    }

    @Override
    public NavigableMap<Bytes32, AccountStorageEntry> storageEntriesFrom(
        final Bytes32 startKeyHash, final int limit) {
      throw new UnsupportedOperationException("Unitries don't associate storage entries to their "
          + "hashes, so they don't support iterating from a given storage entry hash");
    }

    private UInt256 convertToUInt256(final BytesValue value) {
      // TODO: have an optimized method to decode a single scalar since it's used pretty often
      final RLPInput in = RLP.input(value);
      return in.readUInt256Scalar();
    }

    @Override
    public String toString() {
      final StringBuilder builder = new StringBuilder();
      builder.append("AccountState").append("{");
      builder.append("address=").append(getAddress()).append(", ");
      builder.append("nonce=").append(getNonce()).append(", ");
      builder.append("balance=").append(getBalance()).append(", ");
      builder.append("storageRoot=").append(accountValue.getStorageRoot()).append(", ");
      builder.append("codeHash=").append(getCodeHash());
      builder.append("version=").append(getVersion());
      return builder.append("}").toString();
    }
  }

  /**
   * Mutable world updater.
   */
  protected static class Updater extends
      AbstractWorldUpdater<UniTrieMutableWorldState, UniTrieMutableWorldState.WorldStateAccount> {

    protected Updater(final UniTrieMutableWorldState world) {
      super(world);
    }

    @Override
    protected UniTrieMutableWorldState.WorldStateAccount getForMutation(final Address address) {
      final UniTrieMutableWorldState wrapped = wrappedWorldView();
      final BytesValue mappedKey = wrapped.keyMapper.getAccountKey(address);
      return wrapped.trie
          .get(mappedKey)
          .map(bytes -> wrapped.deserializeAccount(address, Hash.hash(address), bytes))
          .orElse(null);
    }

    @Override
    public Collection<Account> getTouchedAccounts() {
      return new ArrayList<>(updatedAccounts());
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
        final BytesValue accountKey = wrapped.keyMapper.getAccountKey(address);
        wrapped.trie.removeRecursive(accountKey);
        wrapped.updatedAccountCode.remove(address);
      }

      for (final UpdateTrackingAccount<UniTrieMutableWorldState.WorldStateAccount> updated :
          updatedAccounts()) {

        Address address = updated.getAddress();
        final UniTrieMutableWorldState.WorldStateAccount origin = updated.getWrappedAccount();

        // Persist updated code if necessary
        Hash accountCodeHash = origin == null ? Hash.EMPTY : origin.getCodeHash();
        if (updated.codeWasUpdated()) {
          accountCodeHash = Hash.hash(updated.getCode());
          wrapped.updatedAccountCode.put(address, updated.getCode());
          wrapped.trie.put(wrapped.keyMapper.getAccountCodeKey(address), updated.getCode());
        }

        // Persist account storage
        Bytes32 accountStorageRoot = UniTrie.NULL_UNINODE_HASH;
        BytesValue storageRootPrefixKey = wrapped.keyMapper.getAccountStoragePrefixKey(address);

        if (updated.getStorageWasCleared()) {
          wrapped.trie.removeRecursive(storageRootPrefixKey);
        }

        final SortedMap<UInt256, UInt256> updatedStorage = updated.getUpdatedStorage();
        if (!updatedStorage.isEmpty()) {
          wrapped.trie.put(storageRootPrefixKey, BytesValue.of(0));
          for (final Map.Entry<UInt256, UInt256> entry : updatedStorage.entrySet()) {
            final UInt256 value = entry.getValue();
            final BytesValue storageKey =
                wrapped.keyMapper.getAccountStorageKey(address, entry.getKey());
            if (value.isZero()) {
              wrapped.trie.remove(storageKey);
            } else {
              wrapped.trie.put(
                  storageKey, RLP.encode(out -> out.writeUInt256Scalar(entry.getValue())));
            }
          }
          accountStorageRoot = wrapped.trie.getHash(storageRootPrefixKey);
       }

        // Finally save the new/updated account
        final BytesValue account =
            serializeAccount(
                updated.getNonce(),
                updated.getBalance(),
                Hash.wrap(accountStorageRoot),
                accountCodeHash,
                updated.getVersion());

        BytesValue accountKey = wrapped.keyMapper.getAccountKey(address);
        wrapped.trie.put(accountKey, account);
      }
    }
  }
}
