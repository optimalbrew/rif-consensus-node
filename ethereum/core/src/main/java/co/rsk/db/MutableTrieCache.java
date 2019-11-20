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

import co.rsk.core.types.ints.Uint24;
import co.rsk.crypto.Keccak256;
import co.rsk.trie.MutableTrie;
import co.rsk.trie.Trie;
import co.rsk.trie.TrieKeySlice;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class MutableTrieCache implements MutableTrie {

    private final TrieKeyMapper trieKeyMapper = new TrieKeyMapper();

    private MutableTrie trie;
    // We use a single cache to mark both changed elements and removed elements.
    // null value means the element has been removed.
    private final Map<BytesValue, Map<Bytes32, BytesValue>> cache;

    // this logs recursive delete operations to be performed at commit time
    private final Set<Bytes32> deleteRecursiveLog;

    public MutableTrieCache(MutableTrie parentTrie) {
        trie = parentTrie;
        cache = new HashMap<>();
        deleteRecursiveLog = new HashSet<>();
    }

    @Override
    public Trie getTrie() {
        assertNoCache();
        return trie.getTrie();
    }

    @Override
    public Keccak256 getHash() {
        assertNoCache();
        return trie.getHash();
    }

    @Override
    public BytesValue get(Bytes32 key) {

        return internalGet(key, trie::get, Function.identity()).orElse(null);
    }


    private <T> Optional<T> internalGet(
            Bytes32 key,
            Function<Bytes32, T> trieRetriever,
            Function<BytesValue, T> cacheTransformer) {

        BytesValue accountWrapper = getAccountWrapper(key);

        Map<Bytes32, BytesValue> accountItems = cache.get(accountWrapper);
        boolean isDeletedAccount = deleteRecursiveLog.contains(accountWrapper);
        if (accountItems == null || !accountItems.containsKey(key)) {
            if (isDeletedAccount) {
                return Optional.empty();
            }
            // uncached account
            return Optional.ofNullable(trieRetriever.apply(key));
        }

        BytesValue cacheItem = accountItems.get(key);
        if (cacheItem == null) {
            // deleted account key
            return Optional.empty();
        }

        // cached account key
        return Optional.ofNullable(cacheTransformer.apply(cacheItem));
    }

    public Iterator<Bytes32> getStorageKeys(Address addr) {
        BytesValue accountStoragePrefixKey = trieKeyMapper.getAccountStoragePrefixKey(addr);
        BytesValue accountWrapper = getAccountWrapper(accountStoragePrefixKey);

        boolean isDeletedAccount = deleteRecursiveLog.contains(accountWrapper);
        Map<Bytes32, BytesValue> accountItems = cache.get(accountWrapper);
        if (accountItems == null && isDeletedAccount) {
            return Collections.emptyIterator();
        }

        if (isDeletedAccount) {
            // lower level is deleted, return cached items
            return new StorageKeysIterator(Collections.emptyIterator(), accountItems, addr, trieKeyMapper);
        }

        Iterator<Bytes32> storageKeys = trie.getStorageKeys(addr);
        if (accountItems == null) {
            // uncached account
            return storageKeys;
        }

        return new StorageKeysIterator(storageKeys, accountItems, addr, trieKeyMapper);
    }

    // This method returns a wrapper with the same content and size expected for a account key
    // when the key is from the same size than the original wrapper, it returns the same object
    private BytesValue getAccountWrapper(BytesValue originalWrapper) {
        byte[] key = originalWrapper.getArrayUnsafe();
        int size = TrieKeyMapper.domainPrefix().size() + TrieKeyMapper.ACCOUNT_KEY_SIZE + TrieKeyMapper.SECURE_KEY_SIZE;
        //return key.length == size ? originalWrapper : new ByteArrayWrapper(Arrays.copyOf(key, size));
        return key.length == size ? originalWrapper : originalWrapper.copyOf(size);

    }


    // This method optimizes cache-to-cache transfers
    @Override
    public void put(Bytes32 key, BytesValue value) {
        // If value==null, do we have the choice to either store it
        // in cache with null or in deleteCache. Here we have the choice to
        // to add it to cache with null value or to deleteCache.
        Map<Bytes32, BytesValue> accountMap = cache.computeIfAbsent(key, k -> new HashMap<>());
        accountMap.put(key, value);
    }

    @Override
    public void put(String key, BytesValue value) {
        Bytes32 keybytes = Bytes32.wrap(key.getBytes(StandardCharsets.UTF_8));
        put(keybytes, value);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // The semantic of implementations is special, and not the same of the MutableTrie
    // It is DELETE ON COMMIT, which means that changes are not applies until commit()
    // is called, and changes are applied last.
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void deleteRecursive(Bytes32 key) {
        // Can there be wrongly unhandled interactions interactions between put() and deleteRecurse()
        // In theory, yes. In practice, never.
        // Suppose that a contract X calls a contract S.
        // Contract S calls itself with CALL.
        // Contract S suicides with SUICIDE opcode.
        // This causes a return to prev contract.
        // But the SUICIDE DOES NOT cause the storage keys to be removed YET.
        // Now parent contract S is still running, and it then can create a new storage cell
        // with SSTORE. This will be stored in the cache as a put(). The cache later receives a
        // deleteRecursive, BUT NEVER IN THE OTHER order.
        // See TransactionExecutor.finalization(), when it iterates the list with getDeleteAccounts().forEach()
        deleteRecursiveLog.add(key);
        cache.remove(key);
    }

    @Override
    public void commit() {
        // in case something was deleted and then put again, we first have to delete all the previous data
        deleteRecursiveLog.forEach(item -> trie.deleteRecursive(item));
        cache.forEach((accountKey, accountData) -> {
            if (accountData != null) {
                // cached account
                accountData.forEach((realKey, value) -> this.trie.put(realKey, value));
            }
        });

        deleteRecursiveLog.clear();
        cache.clear();
    }

    @Override
    public void save() {
        commit();
        trie.save();
    }

    @Override
    public void rollback() {
        cache.clear();
        deleteRecursiveLog.clear();
    }

    @Override
    public Set<BytesValue> collectKeys(int size) {
        Set<BytesValue> parentSet = trie.collectKeys(size);

        // all cached items to be transferred to parent
        cache.forEach((accountKey, account) ->
              account.forEach((realKey, value) -> {
                  if (size == Integer.MAX_VALUE || realKey.size() == size) {
                      if (this.get(realKey) == null) {
                          parentSet.remove(realKey);
                      } else {
                          parentSet.add(realKey);
                      }
                  }
              })
        );
        return parentSet;
    }

    private void assertNoCache() {
        if (!cache.isEmpty()) {
            throw new IllegalStateException();
        }

        if (!deleteRecursiveLog.isEmpty()) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Uint24 getValueLength(Bytes32 key) {
        return internalGet(key, trie::getValueLength, cachedBytes -> new Uint24(cachedBytes.size())).orElse(Uint24.ZERO);
    }

    private static class StorageKeysIterator implements Iterator<Bytes32> {
        private final Iterator<Bytes32> keysIterator;
        private final Map<Bytes32, BytesValue> accountItems;
        private final Address address;
        private final int storageKeyOffset = (
                TrieKeyMapper.domainPrefix().size() +
                TrieKeyMapper.SECURE_ACCOUNT_KEY_SIZE +
                TrieKeyMapper.storagePrefix().size() +
                TrieKeyMapper.SECURE_KEY_SIZE)
                * Byte.SIZE;
        private final TrieKeyMapper trieKeyMapper;
        private Bytes32 currentStorageKey;
        private Iterator<Map.Entry<Bytes32, BytesValue>> accountIterator;

        StorageKeysIterator(
                Iterator<Bytes32> keysIterator,
                Map<Bytes32, BytesValue> accountItems,
                Address addr,
                TrieKeyMapper trieKeyMapper) {
            this.keysIterator = keysIterator;
            this.accountItems = new HashMap<>(accountItems);
            this.address = addr;
            this.trieKeyMapper = trieKeyMapper;
        }

        @Override
        public boolean hasNext() {
            if (currentStorageKey != null) {
                return true;
            }

            while (keysIterator.hasNext()) {
                Bytes32 item = keysIterator.next();
                Bytes32 fullKey = getCompleteKey(item);
                if (accountItems.containsKey(fullKey)) {
                    BytesValue value = accountItems.remove(fullKey);
                    if (value == null){
                        continue;
                    }
                }
                currentStorageKey = item;
                return true;
            }

            if (accountIterator == null) {
                accountIterator = accountItems.entrySet().iterator();
            }

            while (accountIterator.hasNext()) {
                Map.Entry<Bytes32, BytesValue> entry = accountIterator.next();
                Bytes32 key = entry.getKey();
                if (entry.getValue() != null && key.size() * Byte.SIZE > storageKeyOffset) {
                    // cached account key
                    currentStorageKey = getPartialKey(key);
                    return true;
                }
            }

            return false;
        }

        private Bytes32 getPartialKey(Bytes32 key) {
            TrieKeySlice nodeKey = TrieKeySlice.fromKey(key);
            BytesValue storageExpandedKeySuffix = nodeKey.slice(storageKeyOffset, nodeKey.length()).encode();
            return Bytes32.wrap(storageExpandedKeySuffix);
            //return DataWord.valueOf(storageExpandedKeySuffix);
        }

        private Bytes32 getCompleteKey(Bytes32 subkey) {
            return trieKeyMapper.getAccountStorageKey(address, subkey);
        }

        @Override
        public Bytes32 next() {
            if (currentStorageKey == null && !hasNext()) {
                throw new NoSuchElementException();
            }

            Bytes32 next = currentStorageKey;
            currentStorageKey = null;
            return next;
        }
    }
}
