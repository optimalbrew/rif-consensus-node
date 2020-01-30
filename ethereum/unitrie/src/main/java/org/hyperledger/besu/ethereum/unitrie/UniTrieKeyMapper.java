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
package org.hyperledger.besu.ethereum.unitrie;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.plugin.data.Address;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.uint.UInt256;

/**
 * Produce Unitrie keys for accounts, code, and storage.
 *
 * @author ppedemon
 */
public class UniTrieKeyMapper {

  private static final int HASH_DIGEST_PREFIX_SIZE = 10;

  private static final byte DOMAIN_PREFIX = 0;
  private static final byte CODE_PREFIX = (byte) 0x80;
  private static final byte STORAGE_PREFIX = 0;

  // Cache hash digest prefixes to avoid repeated calls to Keccak256 hashing routine
  private Map<BytesValue, BytesValue> digestPrefixCache = new WeakHashMap<>();

  public BytesValue getAccountKey(final Address address) {
    byte[] addressBytes = address.getByteArray();
    ByteBuffer buffer = ByteBuffer.allocate(accountKeySize(addressBytes));
    encodeAccountKey(addressBytes, buffer);
    return BytesValue.wrap(buffer.array());
  }

  public BytesValue getAccountCodeKey(final Address address) {
    byte[] addressBytes = address.getByteArray();
    ByteBuffer buffer = ByteBuffer.allocate(accountCodeKeySize(addressBytes));
    encodeAccountCodeKey(addressBytes, buffer);
    return BytesValue.wrap(buffer.array());
  }

  public BytesValue getAccountStoragePrefixKey(final Address address) {
    byte[] addressBytes = address.getByteArray();
    ByteBuffer buffer = ByteBuffer.allocate(accountStoragePrefixKeySize(addressBytes));
    encodeAccountStoragePrefixKey(addressBytes, buffer);
    return BytesValue.wrap(buffer.array());
  }

  public BytesValue getAccountStorageKey(final Address address, final UInt256 subkey) {
    byte[] addressBytes = address.getByteArray();
    byte[] subkeyBytes = subkey.getByteArray();
    byte[] strippedSubkeyBytes = stripLeadingZeros(subkeyBytes);
    ByteBuffer buffer =
        ByteBuffer.allocate(accountStorageKeySize(addressBytes, strippedSubkeyBytes));
    encodeAccountStorageKey(addressBytes, subkeyBytes, strippedSubkeyBytes, buffer);
    return BytesValue.wrap(buffer.array());
  }

  private int accountKeySize(final byte[] addressBytes) {
    return 1 + HASH_DIGEST_PREFIX_SIZE + addressBytes.length;
  }

  private int accountCodeKeySize(final byte[] addressBytes) {
    return accountKeySize(addressBytes) + 1;
  }

  private int accountStoragePrefixKeySize(final byte[] addressBytes) {
    return accountKeySize(addressBytes) + 1;
  }

  private int accountStorageKeySize(final byte[] addressBytes, final byte[] strippedSubkeyBytes) {
    return accountStoragePrefixKeySize(addressBytes)
        + HASH_DIGEST_PREFIX_SIZE
        + strippedSubkeyBytes.length;
  }

  private void encodeAccountKey(final byte[] addressBytes, final ByteBuffer buffer) {
    buffer.put(DOMAIN_PREFIX);
    buffer.put(hashDigestPrefix(addressBytes).getArrayUnsafe());
    buffer.put(addressBytes);
  }

  private void encodeAccountCodeKey(final byte[] addressBytes, final ByteBuffer buffer) {
    encodeAccountKey(addressBytes, buffer);
    buffer.put(CODE_PREFIX);
  }

  private void encodeAccountStoragePrefixKey(final byte[] addressBytes, final ByteBuffer buffer) {
    encodeAccountKey(addressBytes, buffer);
    buffer.put(STORAGE_PREFIX);
  }

  private void encodeAccountStorageKey(
      final byte[] addressBytes,
      final byte[] subkeyBytes,
      final byte[] strippedSubkeyBytes,
      final ByteBuffer buffer) {

    encodeAccountStoragePrefixKey(addressBytes, buffer);
    buffer.put(hashDigestPrefix(subkeyBytes).getArrayUnsafe());
    buffer.put(strippedSubkeyBytes);
  }

  private synchronized BytesValue hashDigestPrefix(final byte[] value) {
    BytesValue v = BytesValue.wrap(value);

    if (digestPrefixCache.containsKey(v)) {
      return digestPrefixCache.get(v);
    }

    Bytes32 hash = Hash.keccak256(BytesValue.wrap(value));
    BytesValue prefix = hash.slice(0, HASH_DIGEST_PREFIX_SIZE);
    digestPrefixCache.put(v, prefix);
    return prefix;
  }

  private byte[] stripLeadingZeros(final byte[] bytes) {
    int firstNonZero = -1;
    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] != 0) {
        firstNonZero = i;
        break;
      }
    }

    if (firstNonZero == -1) {
      return new byte[] {0};
    }

    if (firstNonZero == 0) {
      return bytes;
    }

    return Arrays.copyOfRange(bytes, firstNonZero, bytes.length);
  }
}
