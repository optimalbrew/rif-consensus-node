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

//import co.rsk.remasc.RemascTransaction;
import co.rsk.utils.Keccak256Helper;
/*import org.ethereum.crypto.Keccak256Helper;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.DataWord;*/
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.bytes.BytesValues;

public class TrieKeyMapper {
    public static final int SECURE_KEY_SIZE = 10;
    //public static final int REMASC_ACCOUNT_KEY_SIZE = SECURE_KEY_SIZE + RemascTransaction.REMASC_ADDRESS.getBytes().length;
    public static final int ACCOUNT_KEY_SIZE = Address.SIZE; //RskAddress.LENGTH_IN_BYTES;
    public static final int SECURE_ACCOUNT_KEY_SIZE = SECURE_KEY_SIZE + ACCOUNT_KEY_SIZE;
    private static final BytesValue DOMAIN_PREFIX = BytesValue.of(0x00);//new byte[] {0x00};
    private static final BytesValue STORAGE_PREFIX = BytesValue.of(0x00);//new byte[] {0x00}; // This makes the MSB 0 be branching
    private static final BytesValue CODE_PREFIX = BytesValue.of(0x80);//new byte[] {(byte) 0x80}; // This makes the MSB 1 be branching

    // This is a performance enhancement. When multiple storage rows for the same
    // contract are stored, the same RskAddress is hashed over and over.
    // We don't need to re-hash it if was hashed last time.
    // The reduction we get is about 50% (2x efficiency)
    private Address lastAddr;
    private BytesValue lastAccountKey;

    public synchronized BytesValue getAccountKey(Address addr) {
        if (!addr.equals(lastAddr)) {
            BytesValue secureKey = secureKeyPrefix(addr);
            lastAccountKey = BytesValues.concatenate(DOMAIN_PREFIX, secureKey, addr); //lastAccountKey = ByteUtil.merge(DOMAIN_PREFIX, secureKey, addr.getBytes());
            lastAddr = addr;
        }

        return lastAccountKey.copy(); //return Arrays.copyOf(lastAccountKey, lastAccountKey.length);
    }

    public BytesValue getCodeKey(Address addr) {
        return BytesValues.concatenate(getAccountKey(addr), CODE_PREFIX);//return ByteUtil.merge(getAccountKey(addr), CODE_PREFIX);
    }

    public BytesValue getAccountStoragePrefixKey(Address addr) {
        return BytesValues.concatenate(getAccountKey(addr), STORAGE_PREFIX);
        //return ByteUtil.merge(getAccountKey(addr), STORAGE_PREFIX);
    }

    public BytesValue getAccountStorageKey(Address addr, Bytes32 subkeyDW) { //DataWord changed for Besu's Bytes32
        // TODO(SDL) should we hash the full subkey or the stripped one?

        BytesValue subkey = subkeyDW;
        BytesValue secureKeyPrefix = secureKeyPrefix(subkey);
        BytesValue storageKey = BytesValues.concatenate(secureKeyPrefix, BytesValue.stripLeadingZeroes(subkey));//ByteUtil.merge(secureKeyPrefix, ByteUtil.stripLeadingZeroes(subkey));
        return BytesValues.concatenate(getAccountStoragePrefixKey(addr), storageKey);//ByteUtil.merge(getAccountStoragePrefixKey(addr), storageKey);
    }

    public BytesValue secureKeyPrefix(BytesValue key) {
        return Keccak256Helper.keccak256(key).copyRange(0, SECURE_KEY_SIZE);
        //return Arrays.copyOfRange(Keccak256Helper.keccak256(key), 0, SECURE_KEY_SIZE);
    }

    public static BytesValue domainPrefix() {
        return DOMAIN_PREFIX.copy(); //
        //return DOMAIN_PREFIX.copyOf(DOMAIN_PREFIX.size());
        //return Arrays.copyOf(DOMAIN_PREFIX, DOMAIN_PREFIX.length);
    }

    public static BytesValue storagePrefix() {
        return  STORAGE_PREFIX.copy();
        //return Arrays.copyOf(STORAGE_PREFIX, STORAGE_PREFIX.length);
    }
}
