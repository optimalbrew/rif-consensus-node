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

import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.AccountStorageEntry;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.InMemoryStorageProvider;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrie;
import org.hyperledger.besu.ethereum.unitrie.UniTrieKeyMapper;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Test;

public class UniTrieMutableWorldStateTest {

  private static final Address ADDRESS =
      Address.fromHexString("0xa94f5374fce5edbc8e2a8697c15331677e6ebf0b");

  private static final UniTrieKeyMapper keyMapper = new UniTrieKeyMapper();

  private static UniTrieMutableWorldState createEmpty(final WorldStateStorage storage) {
    return new UniTrieMutableWorldState(storage);
  }

  private static UniTrieMutableWorldState createEmpty() {
    final InMemoryStorageProvider provider = new InMemoryStorageProvider();
    return createEmpty(provider.createWorldStateStorage());
  }

  @Test
  public void rootHash_Empty() {
    final MutableWorldState worldState = createEmpty();
    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);

    worldState.persist();
    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);
  }

  @Test
  public void containsAccount_AccountDoesNotExist() {
    final WorldState worldState = createEmpty();
    assertThat(worldState.get(ADDRESS)).isNull();
  }

  @Test
  public void containsAccount_AccountExists() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    updater.createAccount(ADDRESS).getMutable().setBalance(Wei.of(100000));
    updater.commit();
    assertThat(worldState.get(ADDRESS)).isNotNull();
    assertThat(worldState.get(ADDRESS).getBalance()).isEqualTo(Wei.of(100000));
    assertThat(worldState.get(ADDRESS).getCodeHash()).isEqualTo(Hash.EMPTY);
  }

  @Test
  public void removeAccount_AccountDoesNotExist() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    updater.deleteAccount(ADDRESS);
    updater.commit();
    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);

    worldState.persist();
    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);
  }

  @Test
  public void removeAccount_UpdatedAccount() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    updater.createAccount(ADDRESS).getMutable().setBalance(Wei.of(100000));
    updater.deleteAccount(ADDRESS);
    updater.commit();
    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);

    worldState.persist();
    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);
  }

  @Test
  public void removeAccount_AccountExists() {
    // Create a world state with one account
    final MutableWorldState worldState = createEmpty();
    WorldUpdater updater = worldState.updater();
    updater.createAccount(ADDRESS).getMutable().setBalance(Wei.of(100000));
    updater.commit();
    assertThat(worldState.get(ADDRESS)).isNotNull();
    assertThat(worldState.rootHash()).isNotEqualTo(UniTrie.NULL_UNINODE_HASH);

    // Delete account
    updater = worldState.updater();
    updater.deleteAccount(ADDRESS);
    assertThat(updater.get(ADDRESS)).isNull();
    assertThat(updater.getAccount(ADDRESS)).isNull();
    updater.commit();
    assertThat(updater.get(ADDRESS)).isNull();

    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);
  }

  @Test
  public void removeAccount_AccountExistsAndIsPersisted() {
    // Create a world state with one account
    final MutableWorldState worldState = createEmpty();
    WorldUpdater updater = worldState.updater();
    updater.createAccount(ADDRESS).getMutable().setBalance(Wei.of(100000));
    updater.commit();
    worldState.persist();
    assertThat(worldState.get(ADDRESS)).isNotNull();
    assertThat(worldState.rootHash()).isNotEqualTo(UniTrie.NULL_UNINODE_HASH);

    // Delete account
    updater = worldState.updater();
    updater.deleteAccount(ADDRESS);
    assertThat(updater.get(ADDRESS)).isNull();
    assertThat(updater.getAccount(ADDRESS)).isNull();
    // Check account is gone after committing
    updater.commit();
    assertThat(updater.get(ADDRESS)).isNull();
    // And after persisting
    worldState.persist();
    assertThat(updater.get(ADDRESS)).isNull();

    assertThat(worldState.rootHash()).isEqualTo(UniTrie.NULL_UNINODE_HASH);
  }

  @Test
  public void commitAndPersist() {
    final KeyValueStorage storage = new InMemoryKeyValueStorage();
    final WorldStateKeyValueStorage kvWorldStateStorage = new WorldStateKeyValueStorage(storage);
    final MutableWorldState worldState = createEmpty(kvWorldStateStorage);
    final WorldUpdater updater = worldState.updater();
    final Wei newBalance = Wei.of(100000);
    final Hash expectedRootHash =
        Hash.fromHexString("0x26ce2edf7658ca8f32316e9c6e2895a6db3161e2cd783a5cc335b47e7ab91c76");

    // Update account and assert we get the expected response from updater
    updater.createAccount(ADDRESS).getMutable().setBalance(newBalance);

    assertThat(updater.get(ADDRESS)).isNotNull();
    assertThat(updater.get(ADDRESS).getBalance()).isEqualTo(newBalance);

    // Commit and check assertions
    updater.commit();
    assertThat(worldState.rootHash()).isEqualTo(expectedRootHash);
    assertThat(worldState.get(ADDRESS)).isNotNull();
    assertThat(worldState.get(ADDRESS).getBalance()).isEqualTo(newBalance);

    // Check that storage is empty before persisting
    assertThat(kvWorldStateStorage.isWorldStateAvailable(worldState.rootHash())).isFalse();

    // Persist and re-run assertions
    worldState.persist();

    assertThat(kvWorldStateStorage.isWorldStateAvailable(worldState.rootHash())).isTrue();
    assertThat(worldState.rootHash()).isEqualTo(expectedRootHash);
    assertThat(worldState.get(ADDRESS)).isNotNull();
    assertThat(worldState.get(ADDRESS).getBalance()).isEqualTo(newBalance);

    // Create new world state and check that it can access modified address
    final MutableWorldState newWorldState =
        new UniTrieMutableWorldState(expectedRootHash, new WorldStateKeyValueStorage(storage));
    assertThat(newWorldState.rootHash()).isEqualTo(expectedRootHash);
    assertThat(newWorldState.get(ADDRESS)).isNotNull();
    assertThat(newWorldState.get(ADDRESS).getBalance()).isEqualTo(newBalance);
  }

  @Test
  public void getAccountNonce_AccountExists() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    updater.createAccount(ADDRESS).getMutable().setNonce(1L);
    updater.commit();
    assertThat(worldState.get(ADDRESS).getNonce()).isEqualTo(1L);
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0x4ac29921daf6f64c5456c91aa9b3eee05efc3f35c2d4ae27068f780112f9560f"));
  }

  @Test
  public void replaceAccountNonce() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setNonce(1L);
    account.setNonce(2L);
    updater.commit();
    assertThat(worldState.get(ADDRESS).getNonce()).isEqualTo(2L);
  }

  @Test
  public void getAccountBalance_AccountExists() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    updater.createAccount(ADDRESS).getMutable().setBalance(Wei.of(100000));
    updater.commit();
    assertThat(worldState.get(ADDRESS).getBalance()).isEqualTo(Wei.of(100000));
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0x26ce2edf7658ca8f32316e9c6e2895a6db3161e2cd783a5cc335b47e7ab91c76"));
  }

  @Test
  public void replaceAccountBalance() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setBalance(Wei.of(200000));
    updater.commit();
    assertThat(worldState.get(ADDRESS).getBalance()).isEqualTo(Wei.of(200000));
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0xa37ff4218d7c9b518e80ae1944f8493080778b6581d365db1fab1fce6e1aad85"));
  }

  @Test
  public void setStorageValue_ZeroValue() {
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(UInt256.ZERO, UInt256.ZERO);
    updater.commit();
    assertThat(worldState.get(ADDRESS).getStorageValue(UInt256.ZERO)).isEqualTo(UInt256.ZERO);
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0x26ce2edf7658ca8f32316e9c6e2895a6db3161e2cd783a5cc335b47e7ab91c76"));
    verifyStoragePrefixRootIsNotPresent(worldState, ADDRESS);
  }

  @Test
  public void setStorageValue_NonzeroValue() {
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(UInt256.ONE, UInt256.valueOf(2));
    updater.commit();
    assertThat(worldState.get(ADDRESS).getStorageValue(UInt256.ONE)).isEqualTo(UInt256.valueOf(2));
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0x28b0bbe83e87bffc213c71b2d9fcf5a6191255d216ea603970c1307bfddccb91"));
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);
  }

  @Test
  public void replaceStorageValue_NonzeroValue() {
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(UInt256.ONE, UInt256.valueOf(2));
    account.setStorageValue(UInt256.ONE, UInt256.valueOf(3));
    updater.commit();
    assertThat(worldState.get(ADDRESS).getStorageValue(UInt256.ONE)).isEqualTo(UInt256.valueOf(3));
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0x8cd6c7bcba0fae12805b3ee5b47a8413cb5b256f8b8a9c40dda512e31be8f176"));
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);
  }

  @Test
  public void replaceStorageValue_ZeroValue() {
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(UInt256.ONE, UInt256.valueOf(2));
    account.setStorageValue(UInt256.ONE, UInt256.ZERO);
    updater.commit();
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0x26ce2edf7658ca8f32316e9c6e2895a6db3161e2cd783a5cc335b47e7ab91c76"));
    verifyStoragePrefixRootIsNotPresent(worldState, ADDRESS);
  }

  @Test
  public void getOriginalStorageValue() {
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater setupUpdater = worldState.updater();
    final MutableAccount setupAccount = setupUpdater.createAccount(ADDRESS).getMutable();
    setupAccount.setStorageValue(UInt256.ONE, UInt256.valueOf(2));
    setupUpdater.commit();

    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.getOrCreate(ADDRESS).getMutable();
    assertThat(account.getOriginalStorageValue(UInt256.ONE)).isEqualTo(UInt256.valueOf(2));

    account.setStorageValue(UInt256.ONE, UInt256.valueOf(3));
    assertThat(account.getOriginalStorageValue(UInt256.ONE)).isEqualTo(UInt256.valueOf(2));
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);
  }

  @Test
  public void originalStorageValueIsAlwaysZeroIfStorageWasCleared() {
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater setupUpdater = worldState.updater();
    final MutableAccount setupAccount = setupUpdater.createAccount(ADDRESS).getMutable();
    setupAccount.setStorageValue(UInt256.ONE, UInt256.valueOf(2));
    setupUpdater.commit();

    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.getOrCreate(ADDRESS).getMutable();

    account.clearStorage();
    assertThat(account.getOriginalStorageValue(UInt256.ONE)).isEqualTo(UInt256.ZERO);
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);
  }

  @Test
  public void clearStorage() {
    final UInt256 storageKey = UInt256.valueOf(1L);
    final UInt256 storageValue = UInt256.valueOf(2L);

    // Create a world state with one account
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(storageKey, storageValue);
    assertThat(account.getStorageValue(storageKey)).isEqualTo(storageValue);

    // Clear storage
    account = updater.getAccount(ADDRESS).getMutable();
    assertThat(account).isNotNull();
    assertThat(account.getStorageValue(storageKey)).isEqualTo(storageValue);
    account.clearStorage();
    assertThat(account.getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);

    // Check storage is cleared after committing
    updater.commit();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);

    // And after persisting
    worldState.persist();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);

    verifyStoragePrefixRootIsNotPresent(worldState, ADDRESS);
  }

  @Test
  public void clearStorage_AfterPersisting() {
    final UInt256 storageKey = UInt256.valueOf(1L);
    final UInt256 storageValue = UInt256.valueOf(2L);

    // Create a world state with one account
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(storageKey, storageValue);
    updater.commit();
    worldState.persist();
    assertThat(worldState.get(ADDRESS)).isNotNull();
    assertThat(worldState.rootHash()).isNotEqualTo(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH);

    // Clear storage
    account = updater.getAccount(ADDRESS).getMutable();
    assertThat(account).isNotNull();
    assertThat(account.getStorageValue(storageKey)).isEqualTo(storageValue);
    account.clearStorage();
    assertThat(account.getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(storageValue);

    // Check storage is cleared after committing
    updater.commit();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);

    // And after persisting
    worldState.persist();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);

    verifyStoragePrefixRootIsNotPresent(worldState, ADDRESS);
  }

  @Test
  public void clearStorageThenEdit() {
    final UInt256 storageKey = UInt256.valueOf(1L);
    final UInt256 originalStorageValue = UInt256.valueOf(2L);
    final UInt256 newStorageValue = UInt256.valueOf(3L);

    // Create a world state with one account
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(storageKey, originalStorageValue);
    assertThat(account.getStorageValue(storageKey)).isEqualTo(originalStorageValue);

    // Clear storage then edit
    account = updater.getAccount(ADDRESS).getMutable();
    assertThat(account).isNotNull();
    assertThat(account.getStorageValue(storageKey)).isEqualTo(originalStorageValue);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(originalStorageValue);
    account.clearStorage();
    account.setStorageValue(storageKey, newStorageValue);
    assertThat(account.getStorageValue(storageKey)).isEqualTo(newStorageValue);

    // Check storage is cleared after committing
    updater.commit();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);

    // And after persisting
    worldState.persist();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);
  }

  @Test
  public void clearStorageThenEditAfterPersisting() {
    final UInt256 storageKey = UInt256.valueOf(1L);
    final UInt256 originalStorageValue = UInt256.valueOf(2L);
    final UInt256 newStorageValue = UInt256.valueOf(3L);

    // Create a world state with one account
    final UniTrieMutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(storageKey, originalStorageValue);
    assertThat(account.getStorageValue(storageKey)).isEqualTo(originalStorageValue);
    updater.commit();
    worldState.persist();
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);

    // Clear storage then edit
    account = updater.getAccount(ADDRESS).getMutable();
    assertThat(account).isNotNull();
    assertThat(account.getStorageValue(storageKey)).isEqualTo(originalStorageValue);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(originalStorageValue);
    account.clearStorage();
    account.setStorageValue(storageKey, newStorageValue);
    assertThat(account.getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(originalStorageValue);

    // Check storage is cleared after committing
    updater.commit();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);

    // And after persisting
    worldState.persist();
    assertThat(updater.getAccount(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(updater.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    assertThat(worldState.get(ADDRESS).getStorageValue(storageKey)).isEqualTo(newStorageValue);
    verifyStoragePrefixRootIsPresent(worldState, ADDRESS);
  }

  @Test
  public void replaceAccountCode() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater = worldState.updater();
    final MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setCode(Bytes.of(1, 2, 3));
    account.setVersion(Account.DEFAULT_VERSION);
    account.setCode(Bytes.of(3, 2, 1));
    updater.commit();
    assertThat(worldState.get(ADDRESS).getCode()).isEqualByComparingTo(Bytes.of(3, 2, 1));
    assertThat(worldState.get(ADDRESS).getVersion()).isEqualTo(Account.DEFAULT_VERSION);
    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0xba71ad6403ddd5984eb4e60d0e57b0ac9855c128447d03025aa95e0848143111"));
  }

  @Test
  public void revert() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater1 = worldState.updater();
    final MutableAccount account1 = updater1.createAccount(ADDRESS).getMutable();
    account1.setBalance(Wei.of(200000));
    updater1.commit();

    final WorldUpdater updater2 = worldState.updater();
    final MutableAccount account2 = updater2.getAccount(ADDRESS).getMutable();
    account2.setBalance(Wei.of(300000));
    assertThat(updater2.get(ADDRESS).getBalance()).isEqualTo(Wei.of(300000));

    updater2.revert();
    assertThat(updater2.get(ADDRESS).getBalance()).isEqualTo(Wei.of(200000));

    updater2.commit();
    assertThat(worldState.get(ADDRESS).getBalance()).isEqualTo(Wei.of(200000));

    assertThat(worldState.rootHash())
        .isEqualTo(
            Hash.fromHexString(
                "0xa37ff4218d7c9b518e80ae1944f8493080778b6581d365db1fab1fce6e1aad85"));
  }

  @Test
  public void shouldReturnNullForGetMutableWhenAccountDeletedInAncestor() {
    final MutableWorldState worldState = createEmpty();
    final WorldUpdater updater1 = worldState.updater();
    final MutableAccount account1 = updater1.createAccount(ADDRESS).getMutable();
    updater1.commit();
    assertThat(updater1.get(ADDRESS))
        .isEqualToComparingOnlyGivenFields(account1, "address", "nonce", "balance", "codeHash");
    updater1.deleteAccount(ADDRESS);

    final WorldUpdater updater2 = updater1.updater();
    assertThat(updater2.get(ADDRESS)).isEqualTo(null);

    final WorldUpdater updater3 = updater2.updater();
    assertThat(updater3.getAccount(ADDRESS)).isEqualTo(null);
  }

  @Test
  public void shouldCombineUnchangedAndChangedValuesWhenRetrievingStorageEntries() {
    final MutableWorldState worldState = createEmpty();
    WorldUpdater updater = worldState.updater();
    MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setBalance(Wei.of(100000));
    account.setStorageValue(UInt256.ONE, UInt256.valueOf(2));
    account.setStorageValue(UInt256.valueOf(2), UInt256.valueOf(5));
    updater.commit();

    final List<AccountStorageEntry> initialEntries = new ArrayList<>();
    initialEntries.add(AccountStorageEntry.forKeyAndValue(UInt256.ONE, UInt256.valueOf(2)));
    initialEntries.add(AccountStorageEntry.forKeyAndValue(UInt256.valueOf(2), UInt256.valueOf(5)));

    updater = worldState.updater();
    account = updater.getAccount(ADDRESS).getMutable();
    account.setStorageValue(UInt256.ONE, UInt256.valueOf(3));
    account.setStorageValue(UInt256.valueOf(3), UInt256.valueOf(6));

    final List<AccountStorageEntry> finalEntries = new ArrayList<>();
    finalEntries.add(AccountStorageEntry.forKeyAndValue(UInt256.ONE, UInt256.valueOf(3)));
    finalEntries.add(AccountStorageEntry.forKeyAndValue(UInt256.valueOf(2), UInt256.valueOf(5)));
    finalEntries.add(AccountStorageEntry.forKeyAndValue(UInt256.valueOf(3), UInt256.valueOf(6)));

    verifyStorage(account, finalEntries);
    verifyStorage(updater.get(ADDRESS), finalEntries);
    verifyStorage(worldState.get(ADDRESS), initialEntries);

    worldState.persist();
    verifyStorage(updater.get(ADDRESS), finalEntries);
    verifyStorage(worldState.get(ADDRESS), initialEntries);

    updater.commit();
    verifyStorage(worldState.get(ADDRESS), finalEntries);

    worldState.persist();
    verifyStorage(worldState.get(ADDRESS), finalEntries);
  }

  @Test
  public void codeLength() {
    final Bytes code = Bytes.fromHexString("0x" + Strings.repeat("123456789abcdef", 1000));

    final MutableWorldState worldState = createEmpty();
    WorldUpdater updater = worldState.updater();

    MutableAccount account = updater.createAccount(ADDRESS).getMutable();
    account.setCode(code);
    updater.commit();

    assertThat(worldState.get(ADDRESS).getCodeSize())
        .isEqualTo(UInt256.valueOf(code.size()).toBytes());
    worldState.persist();
    assertThat(worldState.get(ADDRESS).getCodeSize())
        .isEqualTo(UInt256.valueOf(code.size()).toBytes());
  }

  private void verifyStoragePrefixRootIsPresent(
      final UniTrieMutableWorldState worldState, final Address address) {
    Bytes storagePrefix = keyMapper.getAccountStoragePrefixKey(address);
    assertThat(worldState.getUniTrie().get(storagePrefix)).contains(Bytes.of(0));
  }

  private void verifyStoragePrefixRootIsNotPresent(
      final UniTrieMutableWorldState worldState, final Address address) {
    Bytes storagePrefix = keyMapper.getAccountStoragePrefixKey(address);
    assertThat(worldState.getUniTrie().get(storagePrefix)).isEmpty();
  }

  private void verifyStorage(final Account account, final List<AccountStorageEntry> entries) {
    for (AccountStorageEntry entry : entries) {
      entry
          .getKey()
          .ifPresentOrElse(
              key -> assertThat(account.getStorageValue(key)).isEqualTo(entry.getValue()),
              () -> {
                throw new IllegalStateException("No key in entry");
              });
    }
  }
}
