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
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UniTrieValueTest {

    private final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
    private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory(storage::get);

    @Test
    public void noLongValueInEmptyTrie() {
        UniNode trie = NullUniNode.instance();
        assertThat(trie.getValueWrapper().isLong()).isFalse();
        assertThat(trie.getValueHash()).isEmpty();
        assertThat(trie.getValue()).isEmpty();
    }

    @Test
    public void noLongValueInTrieWithShortValue() {
        BytesValue key = BytesValue.of(1, 1, 0);
        BytesValue value = BytesValue.of(1, 2, 3);

        UniNode trie = NullUniNode.instance()
                .accept(new PutVisitor(value, nodeFactory), key)
                .accept(new GetVisitor(), key);

        assertThat(trie.getValueWrapper().isLong()).isFalse();
        assertThat(trie.getValueWrapper().getHash()).hasValue(Hash.keccak256(value));
        assertThat(trie.getValueLength()).hasValue(UInt24.fromInt(value.size()));
        assertThat(trie.getValue()).hasValue(value);
    }

    @Test
    public void noValueInTrieWith32BytesValue() {
        BytesValue key = BytesValue.of(0, 1);
        BytesValue value = BytesValue.wrap(makeValue(32));

        UniNode trie = NullUniNode.instance()
                .accept(new PutVisitor(value, nodeFactory), key)
                .accept(new GetVisitor(), key);

        assertThat(trie.getValueWrapper().isLong()).isFalse();
        assertThat(trie.getValueHash()).hasValue(Hash.keccak256(value));
        assertThat(trie.getValueLength()).hasValue(UInt24.fromInt(value.size()));
        assertThat(trie.getValue()).hasValue(value);
    }

    @Test
    public void longValueInTrieWith33BytesValue() {
        BytesValue key = BytesValue.of(0, 1);
        BytesValue value = BytesValue.wrap(makeValue(33));

        UniNode trie = NullUniNode.instance()
                .accept(new PutVisitor(value, nodeFactory), key)
                .accept(new GetVisitor(), key);

        assertThat(trie.getValueWrapper().isLong()).isTrue();
        assertThat(trie.getValueHash()).hasValue(Hash.keccak256(value));
        assertThat(trie.getValueLength()).hasValue(UInt24.fromInt(value.size()));
        assertThat(trie.getValue()).hasValue(value);
    }

    private static byte[] makeValue(final int length) {
        byte[] value = new byte[length];

        for (int k = 0; k < length; k++) {
            value[k] = (byte)((k + 1) % 256);
        }

        return value;
    }
}
