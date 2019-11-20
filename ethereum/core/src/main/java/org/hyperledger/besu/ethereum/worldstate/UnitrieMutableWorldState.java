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
package org.hyperledger.besu.ethereum.worldstate;

import co.rsk.core.Repository;
import co.rsk.crypto.Keccak256;
import co.rsk.db.MutableRepository;
import co.rsk.trie.Trie;
import co.rsk.trie.TrieStore;
import org.hyperledger.besu.ethereum.core.*;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPException;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.StoredMerklePatriciaTrie;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.uint.UInt256;

import java.util.*;
import java.util.stream.Stream;

public class UnitrieMutableWorldState implements MutableWorldState {

  private final WorldStateStorage worldStateStorage;
  private final WorldStatePreimageStorage preimageStorage;
  private final MutableRepository repository; // It has the merkle trie inside, which aggregates the worldStateStorage

  private final Map<Address, Map<Bytes32, BytesValue>> updatedStoragePerAccount =
      new HashMap<>(); //Updated state values per account

  private final Map<Address, BytesValue> updatedAccountCode = new HashMap<>(); //Update code per account
    //I think it's a placeholder for new storage keys to be added

  private final Map<Bytes32, UInt256> newStorageKeyPreimages = new HashMap<>();
  //I think its a placeholder for new accounts

  private final Map<Bytes32, Address> newAccountKeyPreimages = new HashMap<>();



  public UnitrieMutableWorldState(final WorldStateStorage storage, final WorldStatePreimageStorage preimageStorage) {
    this(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH, storage, preimageStorage);
  }

  public UnitrieMutableWorldState(
      final Bytes32 rootHash,
      final WorldStateStorage worldStateStorage,
      final WorldStatePreimageStorage preimageStorage) {
    this.worldStateStorage = worldStateStorage;
    this.repository = new MutableRepository(worldStateStorage);
    this.preimageStorage = preimageStorage;
  }

  public UnitrieMutableWorldState(final WorldState worldState) {
    // TODO: this is an abstraction leak (and kind of incorrect in that we reuse the underlying
    // storage), but the reason for this is that the accounts() method is unimplemented below and
    // can't be until NC-754.
    if (!(worldState instanceof UnitrieMutableWorldState)) {
      throw new UnsupportedOperationException();
    }

    final UnitrieMutableWorldState other = (UnitrieMutableWorldState) worldState;
    this.worldStateStorage = other.worldStateStorage;
    this.preimageStorage = other.preimageStorage;
    this.repository = other.repository;
  }

  //AccountState and Account Storage come from the same storage
  private Repository newAccountStateTrie(final Bytes32 rootHash) {
    return new MutableRepository(worldStateStorage);
    return new StoredMerklePatriciaTrie<>(
        worldStateStorage::getAccountStateTrieNode, rootHash, b -> b, b -> b);
  }

  @Override
  public Hash rootHash() {
    return Hash.wrap(repository.getRoot());
  }

  @Override
  public MutableWorldState copy() {
    return new UnitrieMutableWorldState(rootHash(), worldStateStorage, preimageStorage);
  }

  @Override
  public Account get(final Address address) {
    return repository.getAccountState(address);
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
      final int version) {
    final StateTrieAccountValue accountValue =
        new StateTrieAccountValue(nonce, balance, version);
    return RLP.encode(accountValue::writeTo);
  }

  @Override
  public WorldUpdater updater() {
    return new Updater(this);
  }

  @Override
  public Stream<StreamableAccount> streamAccounts(final Bytes32 startKeyHash, final int limit) {
    return accountStateTrie.entriesFrom(startKeyHash, limit).entrySet().stream()
        .map(
            entry -> {
              final Optional<Address> address = getAccountTrieKeyPreimage(entry.getKey());
              final AccountState account =
                  deserializeAccount(
                      address.orElse(Address.ZERO), Hash.wrap(entry.getKey()), entry.getValue());
              return new StreamableAccount(address, account);
            });
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rootHash());
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof UnitrieMutableWorldState)) {
      return false;
    }

    final UnitrieMutableWorldState that = (UnitrieMutableWorldState) other;
    return this.rootHash().equals(that.rootHash());
  }

  @Override
  public void persist() {
    final WorldStateStorage.Updater stateUpdater = worldStateStorage.updater();
    // Store updated code
    for (final BytesValue code : updatedAccountCode.values()) {
      stateUpdater.putCode(code);
    }
    // Commit account storage tries
    //TODO this does not exist in unitrie
    for (final MerklePatriciaTrie<Bytes32, BytesValue> updatedStorage :
        updatedStorageTries.values()) {
      updatedStorage.commit(stateUpdater::putAccountStorageTrieNode);
    }
    // Commit account updates
    //TODO ACCOUNT STATE TRIE AND STORAGE TRIE MUST BE THE SAME
    //accountStateTrie.commit(stateUpdater::putAccountStateTrieNode);

    // Persist preimages
    final WorldStatePreimageStorage.Updater preimageUpdater = preimageStorage.updater();
    newStorageKeyPreimages.forEach(preimageUpdater::putStorageTrieKeyPreimage);
    //newAccountKeyPreimages.forEach(preimageUpdater::putAccountTrieKeyPreimage);

    // Clear pending changes that we just flushed
    updatedStoragePerAccount.clear();
    updatedAccountCode.clear();
    newStorageKeyPreimages.clear();

    // Push changes to underlying storage
    preimageUpdater.commit();
    stateUpdater.commit();
  }

  private Optional<UInt256> getStorageTrieKeyPreimage(final Bytes32 trieKey) {
    return Optional.ofNullable(newStorageKeyPreimages.get(trieKey))
        .or(() -> preimageStorage.getStorageTrieKeyPreimage(trieKey));
  }

  private Optional<Address> getAccountTrieKeyPreimage(final Bytes32 trieKey) {
    return Optional.ofNullable(newAccountKeyPreimages.get(trieKey))
        .or(() -> preimageStorage.getAccountTrieKeyPreimage(trieKey));
  }

  // An immutable class that represents an individual account as stored in
  // in the world state's underlying merkle patricia trie.
  protected class WorldStateAccount implements Account {

    private final Address address;
    private final Hash addressHash;

    final StateTrieAccountValue accountValue; //TODO Storage root removed

    // Lazily initialized since we don't always access storage.
    private volatile MerklePatriciaTrie<Bytes32, BytesValue> storageTrie;

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
      final BytesValue updatedCode = updatedAccountCode.get(address);
      if (updatedCode != null) {
        return updatedCode;
      }
      return repository.getCode(address);
    }

    @Override
    public boolean hasCode() {
      return !getCode().isEmpty();
    }

    @Override
    public int getVersion() {
      return accountValue.getVersion();
    }

    @Override
    public UInt256 getStorageValue(final UInt256 key) {

      BytesValue storageValue = repository.getStorageValue(address, Hash.hash(key.getBytes()));

      if(storageValue == null){
        return UInt256.ZERO;
      }
      return convertToUInt256(storageValue);
    }

    @Override
    public UInt256 getOriginalStorageValue(final UInt256 key) {
      return getStorageValue(key);
    }


    private UInt256 convertToUInt256(final BytesValue value) {
      // TODO: we could probably have an optimized method to decode a single scalar since it's used
      // pretty often.
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
      builder.append("version=").append(getVersion());
      return builder.append("}").toString();
    }
  }

  protected static class Updater
      extends AbstractWorldUpdater<UnitrieMutableWorldState, WorldStateAccount> {



    protected Updater(final UnitrieMutableWorldState world) {
      super(world);
    }


    @Override
    protected WorldStateAccount getForMutation(final Address address) {
      final UnitrieMutableWorldState wrapped = wrappedWorldView();
      final Hash addressHash = Hash.hash(address);
      return wrapped
          .accountStateTrie
          .get(addressHash)
          .map(bytes -> wrapped.deserializeAccount(address, addressHash, bytes))
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
      wrappedWorldView().repository.rollback(); //TODO check how this is used in unitrie
    }

    @Override
    public void commit() {
      final UnitrieMutableWorldState wrapped = wrappedWorldView();

      for (final Address address : deletedAccounts()) {
        wrapped.repository.delete(address);
      }

      for (final UpdateTrackingAccount<WorldStateAccount> updated : updatedAccounts()) {
        final WorldStateAccount origin = updated.getWrappedAccount();

        // Save the code in key-value storage ...
        if (updated.codeWasUpdated()) {
          wrapped.repository.saveCode(updated.getAddress(), updated.getCode());
        }
        final boolean freshState = origin == null || updated.getStorageWasCleared();
        if (freshState) {
            wrapped.updatedStoragePerAccount.remove(updated.getAddress());
        }

        final SortedMap<UInt256, UInt256> updatedStorage = updated.getUpdatedStorage();

        if (!updatedStorage.isEmpty()) {
          // Apply any storage updates
          for (final Map.Entry<UInt256, UInt256> entry : updatedStorage.entrySet()) {

            final Hash keyHash = Hash.hash(entry.getKey().getBytes());
            //if (value.isZero()) { //Zero means "delete the entry"
              //wrapped.repository.delete(); TODO ASK SERGIO: DELETE VALUE IS NOT IMPLEMENTED IN UNITRIE! so DELETE WOULD BE JUST PUTTING VALUE 0, so in this case doing nothing
            //} else {
            wrapped.newStorageKeyPreimages.put(keyHash, entry.getKey());
            wrapped.repository.addStorageBytes(updated.getAddress(), keyHash, RLP.encode(out -> out.writeUInt256Scalar(entry.getValue())));
            //}
          }
        }

        // Save address preimage //TODO INVESTIGATE WHAT'S THIS PREIMAGE STUFF
        //wrapped.newAccountKeyPreimages.put(updated.getAddressHash(), updated.getAddress());


        // Lastly, save the new account.
        final BytesValue account =
            serializeAccount(
                updated.getNonce(),
                updated.getBalance(),
                updated.getVersion());

        wrapped.repository.updateAccountState(updated.getAddress(), account);
        wrapped.repository.save();
      }
    }
  }
}
