/*
 * This file is part of RskJ
 * Copyright (C) 2019 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.db;

import co.rsk.core.Coin;
import co.rsk.core.Repository;
import org.hyperledger.besu.ethereum.core.UnitrieAccountState;
import co.rsk.core.types.ints.Uint24;
import co.rsk.crypto.Keccak256;
import co.rsk.trie.*;
import co.rsk.utils.ByteUtils;
import com.google.common.annotations.VisibleForTesting;

import org.hyperledger.besu.ethereum.core.*;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;


import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class MutableRepository implements Repository {
    //private static final Logger logger = LoggerFactory.getLogger("repository");
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final byte[] ONE_BYTE_ARRAY = new byte[] { 0x01 };

    private final TrieKeyMapper trieKeyMapper;

    private final MutableTrie mutableTrie;

    private final UnitrieWorldUpdater worldUpdater;

    public MutableRepository(WorldStateStorage storage, WorldUpdater worldUpdater) {
        this.trieKeyMapper = new TrieKeyMapper();
        TrieStore ts = new TrieStoreImpl(worldUpdater);
        this.mutableTrie = new MutableTrieImpl(ts, new Trie(ts));
        this.worldUpdater = worldUpdater;
    }

    @Override
    public Trie getTrie() {
        return mutableTrie.getTrie();
    }


    @Override
    public synchronized void setupContract(Address addr) {
        Bytes32 prefix = trieKeyMapper.getAccountStoragePrefixKey(addr);
        mutableTrie.put(prefix, BytesValue.wrap(ONE_BYTE_ARRAY));
    }


    public synchronized boolean isExist(Address addr) {
        // Here we assume size != 0 means the account exists
        return mutableTrie.getValueLength(trieKeyMapper.getAccountKey(addr)).compareTo(Uint24.ZERO) > 0;
    }

    @Override
    public synchronized AccountState getAccountState(Address addr) {

        //UNITRIE_NOTES : This would be the way of getting the account in Besu.
        //In besu Account IS an AccountState (AccountState is the readonly interface or everything but the address, added in Account)

        return this.worldUpdater.getAccount(addr);

       /* AccountState result = null;
        byte[] accountData = getAccountData(addr);

        // If there is no account it returns null
        if (accountData != null && accountData.length != 0) {
            result = new AccountState(accountData);
        }
        return result;*/
    }

    public synchronized void delete(Address addr) {
        mutableTrie.deleteRecursive(trieKeyMapper.getAccountKey(addr));
    }

    //UNITRIE_NOTES
    //This might not be initially supported
    /*@Override
    public synchronized void hibernate(Address addr) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.hibernate();
        updateAccountState(addr, account);
    }*/

    //UNITRIE_NOTES, the way to do it now is
    //MutableAccount::setNonce
    /*@Override
    public void setNonce(Address addr, BigInteger nonce) {
        AccountState account = getAccountStateOrCreateNew(addr);
        account.setNonce(nonce);
        updateAccountState(addr, account);
    }*/

    //UNITRIE_NOTES,  the way to do it now is
    // MutableAccount::incrementNonce()
    /*@Override
    public synchronized BigInteger increaseNonce(Address addr) {
        AccountState account = getAccountStateOrCreateNew(addr);
        account.incrementNonce();
        updateAccountState(addr, account);
        return account.getNonce();
    }*/

    @Override
    public synchronized BigInteger getNonce(Address addr) {
        // Why would getNonce create an Account in the repository? The semantic of a get()
        // is clear: do not change anything!


        AccountState account = getAccountState(addr);
        if (account == null) {
            return BigInteger.ZERO;
        }

        return account.getNonce();
    }

    public synchronized void saveCode(Address addr, BytesValue code) {
        Bytes32 key = trieKeyMapper.getCodeKey(addr);
        mutableTrie.put(key, code);

        if (code != null && code.size() != 0 && !isExist(addr)) {
            createAccount(addr);
        }
    }

    @Override
    public synchronized int getCodeLength(Address addr) {
        AccountState account = getAccountState(addr);
        if (account == null) {
            return 0;
        }

        Bytes32 key = trieKeyMapper.getCodeKey(addr);
        return mutableTrie.getValueLength(key).intValue();
    }

    @Override
    public synchronized BytesValue getCode(Address addr) {

        if (!isExist(addr)) {
            return BytesValue.EMPTY;
        }

        AccountState account = getAccountState(addr);

        Bytes32 key = trieKeyMapper.getCodeKey(addr);
        BytesValue code = mutableTrie.get(key);
        return code;
    }

    @Override
    public Optional<BytesValue> getCode(final Bytes32 codeHash) {

        if (codeHash.equals(Hash.EMPTY)) {
            return Optional.of(BytesValue.EMPTY);
        } else {
            return Optional.ofNullable(mutableTrie.get(codeHash));
        }
    }

    public boolean isContract(Address addr) {
        Bytes32 prefix = trieKeyMapper.getAccountStoragePrefixKey(addr);
        return mutableTrie.get(prefix) != null;
    }

    public synchronized void addStorageRow(Address addr, Bytes32 key, Bytes32 value) {
        // DataWords are stored stripping leading zeros.
        addStorageBytes(addr, key, ByteUtils.stripLeadingZeroes(value));
    }

    @Override
    public synchronized void addStorageBytes(Address addr, Bytes32 key, BytesValue value) {
        // This should not happen in production because contracts are created before storage cells are added to them.
        // But it happens in Repository tests, that create only storage row cells.
        if (!isExist(addr)) {
            createAccount(addr);
            setupContract(addr);
        }

        byte[] triekey = trieKeyMapper.getAccountStorageKey(addr, key);

        // Special case: if the value is an empty vector, we pass "null" which commands the trie to remove the item.
        // Note that if the call comes from addStorageRow(), this method will already have replaced 0 by null, so the
        // conversion here only applies if this is called directly. If suppose this only occurs in tests, but it can
        // also occur in precompiled contracts that store data directly using this method.
        if (value == null || value.length == 0) {
            mutableTrie.put(triekey, null);
        } else {
            mutableTrie.put(triekey, value);
        }
    }

    @Override
    public synchronized BytesValue getStorageValue(Address addr, Bytes32 key) {
        BytesValue triekey = trieKeyMapper.getAccountStorageKey(addr, key);
        BytesValue value = mutableTrie.get(triekey).orElse(null);
        return value;
    }

    @Override
    public synchronized byte[] getStorageBytes(Address addr, Bytes32 key) {
        byte[] triekey = trieKeyMapper.getAccountStorageKey(addr, key);
        return mutableTrie.get(triekey);
    }

    @Override
    public Iterator<Bytes32> getStorageKeys(Address addr) {
        // -1 b/c the first bit is implicit in the storage node
        return mutableTrie.getStorageKeys(addr);
    }

    @Override
    public int getStorageKeysCount(Address addr) {
        // FIXME(diegoll): I think it's kind of insane to iterate the whole tree looking for storage keys for this address
        //  I think we can keep a counter for the keys, using the find function for detecting duplicates and so on
        int storageKeysCount = 0;
        Iterator<DataWord> keysIterator = getStorageKeys(addr);
        for(;keysIterator.hasNext(); keysIterator.next()) {
            storageKeysCount ++;
        }
        return storageKeysCount;
    }

    @Override
    public synchronized Coin getBalance(Address addr) {
        AccountState account = getAccountState(addr);
        return (account == null) ? Coin.ZERO: account.getBalance();
    }

    @Override
    public synchronized Coin addBalance(Address addr, Coin value) {
        AccountState account = getAccountStateOrCreateNew(addr);

        Coin result = account.addToBalance(value);
        updateAccountState(addr, account);

        return result;
    }

    @Override
    public synchronized Set<Address> getAccountsKeys() {
        Set<Address> result = new HashSet<>();
        //TODO(diegoll): this is needed when trie is a MutableTrieCache, check if makes sense to commit here
        mutableTrie.commit();
        Trie trie = mutableTrie.getTrie();
        Iterator<Trie.IterationElement> preOrderIterator = trie.getPreOrderIterator();
        while (preOrderIterator.hasNext()) {
            TrieKeySlice nodeKey = preOrderIterator.next().getNodeKey();
            int nodeKeyLength = nodeKey.length();
            if (nodeKeyLength == (1 + TrieKeyMapper.SECURE_KEY_SIZE + Address.SIZE) * Byte.SIZE) {
                byte[] address = nodeKey.slice(nodeKeyLength - Address.SIZE * Byte.SIZE, nodeKeyLength).encode();
                result.add(new Address(address));
            }
        }
        return result;
    }

    // To start tracking, a new repository is created, with a MutableTrieCache in the middle
    @Override
    public synchronized Repository startTracking() {
        return new MutableRepository(new MutableTrieCache(mutableTrie));
    }

    @Override
    public void save() {
        mutableTrie.save();
    }

    @Override
    public synchronized void commit() {
        mutableTrie.commit();
    }

    @Override
    public synchronized void rollback() {
        mutableTrie.rollback();
    }

    @Override
    public synchronized Keccak256 getRoot() {
        mutableTrie.save();

        Keccak256 rootHash = mutableTrie.getHash();
        //logger.trace("getting repository root hash {}", rootHash);
        return rootHash;
    }

    //UNITRIE_NOTES: It seems we don't need this, since Besu's Mutable Account already has all the needed primitives for updating
    //Specifically UpdateTrackingAccount
    /*@Override
    public synchronized void updateAccountState(Address addr, AccountState accountState) {
        Bytes32 accountKey = trieKeyMapper.getAccountKey(addr);
        mutableTrie.put(accountKey, accountState);
    }*/

    @VisibleForTesting
    public Keccak256 getStorageStateRoot(Address addr) {
        Bytes32 prefix = trieKeyMapper.getAccountStoragePrefixKey(addr);

        // The value should be ONE_BYTE_ARRAY, but we don't need to check nothing else could be there.
        Trie storageRootNode = mutableTrie.getTrie().find(prefix);
        if (storageRootNode == null) {
            return new Keccak256(Hash.EMPTY_TRIE_HASH);
        }

        // Now it's a bit tricky what to return: if I return the storageRootNode hash then it's counting the "0x01"
        // value, so the try one gets will never match the trie one gets if creating the trie without any other data.
        // Unless the PDV trie is used. The best we can do is to return storageRootNode hash
        return storageRootNode.getHash();
    }

    @Nonnull
    private synchronized AccountState getAccountStateOrCreateNew(Address addr) {
        AccountState account = getAccountState(addr);
        return (account == null) ? createAccount(addr) : account;
    }

    private BytesValue getAccountData(Address addr) {
        return mutableTrie.get(trieKeyMapper.getAccountKey(addr));
    }
}
