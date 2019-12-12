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
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UniTrieEncodingTest {

    private final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
    private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory(storage::get);

    @Test
    public void emptyTree_givesNullEncoding() {
        assertThat(NullUniNode.instance().getEncoding()).isEqualTo(UniNodeEncoding.NULL_UNINODE_ENCODING);
    }

    @Test
    public void emptyLeaf_encodesAsNullUniNode() {
        assertThat(nodeFactory.createLeaf(BytesValue.EMPTY, ValueWrapper.EMPTY).getEncoding())
                .isEqualTo(UniNodeEncoding.NULL_UNINODE_ENCODING);
    }

    @Test
    public void emptyPath_encodesCorrectly() {
        BytesValue value = BytesValue.of(1, 2, 3);
        UniNode trie = nodeFactory.createLeaf(BytesValue.EMPTY, ValueWrapper.fromValue(value));
        assertThat(trie.getEncoding()).isEqualTo(BytesValue.of(0x40).concat(value));
    }

    @Test
    public void leaf_encodesCorrectly() {
        BytesValue path = BytesValue.of(1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0);
        BytesValue value = BytesValue.of(1, 2, 3);
        UniNode trie = nodeFactory.createLeaf(path, ValueWrapper.fromValue(value));
        assertThat(trie.getEncoding()).isEqualTo(BytesValue.of(0x50, 0x0a, 0xaf, 0x40).concat(value));
    }

    @Test
    public void embeddedChildren_encodesCorrectly() {
        BytesValue valueTop = BytesValue.of(9);
        BytesValue valueLeft = BytesValue.of(1, 2, 3, 4);
        BytesValue valueRight = BytesValue.of(5, 6, 7, 8);
        UniNode trie = NullUniNode.instance()
                .accept(new PutVisitor(valueLeft, nodeFactory), BytesValue.of(1, 1, 0, 0))
                .accept(new PutVisitor(valueRight, nodeFactory), BytesValue.of(1, 1, 1, 1))
                .accept(new PutVisitor(valueTop, nodeFactory), BytesValue.of(1, 1));
        assertThat(trie.getEncoding())
                .isEqualTo(BytesValue.fromHexString("0x5f01c0075000000102030407500080050607080e09"));
    }

    @Test
    public void longChildren_encodesCorrectly() {
        Bytes32 hash1 = Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555555");
        Bytes32 hash2 = Bytes32.fromHexString("0x5655555555555555555555555555555555555555555555555555555555555556");

        BytesValue path1 = PathEncoding.decodePath(hash1, 256);
        BytesValue path2 = PathEncoding.decodePath(hash2, 256);

        BytesValue value = BytesValue.wrap(makeValue(1000));
        UniNode trie = NullUniNode.instance()
                .accept(new PutVisitor(value, nodeFactory), path1)
                .accept(new PutVisitor(value, nodeFactory), path2)
                .accept(new PutVisitor(BytesValue.of(9), nodeFactory), BytesValue.of(0, 1, 0, 1, 0, 1));

        BytesValue enc = trie.getEncoding();

        // No long val, has path, has left and right children, none embedded
        assertThat(enc.get(0)).isEqualTo((byte)0b01011100);

        // Path length is 6 (encoded as 6-1 = 5)
        assertThat(enc.get(1)).isEqualTo((byte)5);

        // Root node encoded path must be common part of {0x55, 0x56} = 0b010101
        assertThat(PathEncoding.decodePath(enc.slice(2, 1), enc.get(1) + 1))
                .isEqualTo(BytesValue.of(0, 1, 0, 1, 0, 1));

        // Value must be 9, at the end of the encoding
        assertThat(enc.get(enc.size() - 1)).isEqualTo((byte)9);
    }

    private static byte[] makeValue(final int length) {
        byte[] value = new byte[length];

        for (int k = 0; k < length; k++) {
            value[k] = (byte)((k + 1) % 256);
        }

        return value;
    }
}
