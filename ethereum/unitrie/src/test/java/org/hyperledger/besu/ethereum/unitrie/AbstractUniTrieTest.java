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
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.trie.Proof;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractUniTrieTest {

    MerklePatriciaTrie<BytesValue, String> trie;

    abstract MerklePatriciaTrie<BytesValue, String> createTrie();

    @Before
    public void setup() {
        trie = createTrie();
    }

    @Test
    public void emptyTreeReturnsEmpty() {
        assertFalse(trie.get(BytesValue.EMPTY).isPresent());
    }

    @Test
    public void emptyTreeHasKnownRootHash() {
        assertThat(trie.getRootHash().toString()).isEqualTo(UniNodeEncoding.NULL_UNINODE_HASH.toString());
    }

    @Test(expected = NullPointerException.class)
    public void throwsOnUpdateWithNull() {
        trie.put(BytesValue.EMPTY, null);
    }

    @Test
    public void replaceSingleValue() {
        final BytesValue key = BytesValue.of(1);
        final String value1 = "value1";
        trie.put(key, value1);
        assertThat(trie.get(key)).isEqualTo(Optional.of(value1));

        final String value2 = "value2";
        trie.put(key, value2);
        assertThat(trie.get(key)).isEqualTo(Optional.of(value2));
    }

    @Test
    public void hashChangesWhenSingleValueReplaced() {
        final BytesValue key = BytesValue.of(1);
        final String value1 = "value1";
        trie.put(key, value1);
        final Bytes32 hash1 = trie.getRootHash();

        final String value2 = "value2";
        trie.put(key, value2);
        final Bytes32 hash2 = trie.getRootHash();

        assertThat(hash1).isNotEqualTo(hash2);

        trie.put(key, value1);
        assertThat(trie.getRootHash()).isEqualTo(hash1);
    }

    @Test
    public void readPastLeaf() {
        final BytesValue key1 = BytesValue.of(1);
        trie.put(key1, "value");
        final BytesValue key2 = BytesValue.of(16);
        assertFalse(trie.get(key2).isPresent());
    }

    @Test
    public void branchValue() {
        final BytesValue key1 = BytesValue.of(1);
        final BytesValue key2 = BytesValue.of(1, 1);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value1));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));
    }

    @Test
    public void readPastBranch() {
        final BytesValue key1 = BytesValue.of(12);
        final BytesValue key2 = BytesValue.of(12, 54);

        final String value1 = "value1";
        trie.put(key1, value1);
        final String value2 = "value2";
        trie.put(key2, value2);

        final BytesValue key3 = BytesValue.of(3);
        assertFalse(trie.get(key3).isPresent());
    }

    @Test
    public void branchWithValue() {
        final BytesValue key1 = BytesValue.of(5);
        final BytesValue key2 = BytesValue.EMPTY;

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value1));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));
    }

    @Test
    public void childOfBranch() {
        final BytesValue key1 = BytesValue.of(1, 5, 9);
        final BytesValue key2 = BytesValue.of(1, 5, 2);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value1));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));
        assertFalse(trie.get(BytesValue.of(1, 4)).isPresent());
    }

    @Test
    public void branchOnTop() {
        final BytesValue key1 = BytesValue.of(0xfe, 1);
        final BytesValue key2 = BytesValue.of(0xfe, 2);
        final BytesValue key3 = BytesValue.of(0xe1, 1);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        final String value3 = "value3";
        trie.put(key3, value3);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value1));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));
        assertThat(trie.get(key3)).isEqualTo(Optional.of(value3));
        assertFalse(trie.get(BytesValue.of(1, 4)).isPresent());
        assertFalse(trie.get(BytesValue.of(2, 4)).isPresent());
        assertFalse(trie.get(BytesValue.of(3)).isPresent());
    }

    @Test
    public void splitBranches() {
        final BytesValue key1 = BytesValue.of(1, 5, 9);
        final BytesValue key2 = BytesValue.of(1, 5, 2);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        final BytesValue key3 = BytesValue.of(1, 9, 1);

        final String value3 = "value3";
        trie.put(key3, value3);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value1));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));
        assertThat(trie.get(key3)).isEqualTo(Optional.of(value3));
    }

    @Test
    public void replaceBranchChild() {
        final BytesValue key1 = BytesValue.of(0);
        final BytesValue key2 = BytesValue.of(1);

        final String value1 = "value1";
        trie.put(key1, value1);
        final String value2 = "value2";
        trie.put(key2, value2);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value1));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));

        final String value3 = "value3";
        trie.put(key1, value3);

        assertThat(trie.get(key1)).isEqualTo(Optional.of(value3));
        assertThat(trie.get(key2)).isEqualTo(Optional.of(value2));
    }

    @Test
    public void forceCoalesce() {
        final BytesValue key1 = BytesValue.of(0);
        final BytesValue key2 = BytesValue.of(1);
        final BytesValue key3 = BytesValue.of(2);
        final BytesValue key4 = BytesValue.of(0, 0);
        final BytesValue key5 = BytesValue.of(0, 1);

        trie.put(key1, "value1");
        trie.put(key2, "value2");
        trie.put(key3, "value3");
        trie.put(key4, "value4");
        trie.put(key5, "value5");

        trie.remove(key2);
        trie.remove(key3);

        assertThat(trie.get(key1)).isEqualTo(Optional.of("value1"));
        assertFalse(trie.get(key2).isPresent());
        assertFalse(trie.get(key3).isPresent());
        assertThat(trie.get(key4)).isEqualTo(Optional.of("value4"));
        assertThat(trie.get(key5)).isEqualTo(Optional.of("value5"));
    }

    @Test
    public void removeUnexistingNodeHasNoEffect() {
        final BytesValue key1 = BytesValue.of(1, 5, 9);
        final BytesValue key2 = BytesValue.of(1, 5, 2);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        final BytesValue hash = trie.getRootHash();

        trie.remove(BytesValue.of(1, 4));
        assertThat(trie.getRootHash()).isEqualTo(hash);
    }

    @Test
    public void hashChangesWhenValueChanged() {
        final BytesValue key1 = BytesValue.of(1, 5, 8, 9);
        final BytesValue key2 = BytesValue.of(1, 6, 1, 2);
        final BytesValue key3 = BytesValue.of(1, 6, 1, 3);

        final String value1 = "value1";
        trie.put(key1, value1);
        final Bytes32 hash1 = trie.getRootHash();

        final String value2 = "value2";
        trie.put(key2, value2);
        final String value3 = "value3";
        trie.put(key3, value3);
        final Bytes32 hash2 = trie.getRootHash();

        assertThat(hash1).isNotEqualTo(hash2);

        final String value4 = "value4";
        trie.put(key1, value4);
        final Bytes32 hash3 = trie.getRootHash();

        assertThat(hash1).isNotEqualTo(hash3);
        assertThat(hash2).isNotEqualTo(hash3);

        trie.put(key1, value1);
        assertThat(trie.getRootHash()).isEqualTo(hash2);

        trie.remove(key2);
        trie.remove(key3);
        assertThat(trie.getRootHash()).isEqualTo(hash1);
    }

    @Test
    public void getValueWithProof_emptyTrie() {
        final BytesValue key1 = BytesValue.of(0xfe, 1);

        Proof<String> valueWithProof = trie.getValueWithProof(key1);
        assertThat(valueWithProof.getValue()).isEmpty();
        assertThat(valueWithProof.getProofRelatedNodes()).hasSize(0);
    }

    @Test
    public void getValueWithProof_forExistingValues() {
        final BytesValue key1 = BytesValue.of(0xfe, 1);
        final BytesValue key2 = BytesValue.of(0xfe, 2);
        final BytesValue key3 = BytesValue.of(0xfe, 3);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        final String value3 = "value3";
        trie.put(key3, value3);

        final Proof<String> valueWithProof = trie.getValueWithProof(key2);
        assertThat(valueWithProof.getProofRelatedNodes()).hasSize(2);
        assertThat(valueWithProof.getValue()).contains(value2);

        MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
        List<UniNode> nodes = new UniTrieNodeDecoder(storage).decodeNodes(valueWithProof.getProofRelatedNodes().get(1));

        assertThat(new String(nodes.get(1).getValue().get().extractArray(), StandardCharsets.UTF_8)).isEqualTo(value2);
        assertThat(new String(nodes.get(2).getValue().get().extractArray(), StandardCharsets.UTF_8)).isEqualTo(value3);
    }

    @Test
    public void getValueWithProof_forNonExistentValue() {
        final BytesValue key1 = BytesValue.of(0xfe, 1);
        final BytesValue key2 = BytesValue.of(0xfe, 2);
        final BytesValue key3 = BytesValue.of(0xfe, 3);
        final BytesValue key4 = BytesValue.of(0xfe, 4);

        final String value1 = "value1";
        trie.put(key1, value1);

        final String value2 = "value2";
        trie.put(key2, value2);

        final String value3 = "value3";
        trie.put(key3, value3);

        final Proof<String> valueWithProof = trie.getValueWithProof(key4);
        assertThat(valueWithProof.getValue()).isEmpty();
        assertThat(valueWithProof.getProofRelatedNodes()).hasSize(1);
    }

    @Test
    public void getValueWithProof_singleNodeTrie() {
        final BytesValue key1 = BytesValue.of(0xfe, 1);
        final String value1 = "1";
        trie.put(key1, value1);

        final Proof<String> valueWithProof = trie.getValueWithProof(key1);
        assertThat(valueWithProof.getValue()).contains(value1);
        assertThat(valueWithProof.getProofRelatedNodes()).hasSize(1);

        MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
        List<UniNode> nodes = new UniTrieNodeDecoder(storage).decodeNodes(valueWithProof.getProofRelatedNodes().get(0));

        assertThat(nodes.size()).isEqualTo(1);
        final String nodeValue = new String(nodes.get(0).getValue().get().extractArray(), StandardCharsets.UTF_8);
        assertThat(nodeValue).isEqualTo(value1);
    }
}
