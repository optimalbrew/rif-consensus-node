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

import org.hyperledger.besu.ethereum.trie.BasicNode;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.NodeUpdater;
import org.hyperledger.besu.ethereum.trie.Proof;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple in-memory Unitrie.
 *
 * @param <K>  key type
 * @param <V>  value type
 * @author ppedemon
 */
public class SimpleUniTrie<K extends BytesValue, V> implements MerklePatriciaTrie<K, V> {

    private final GetVisitor getVisitor = new GetVisitor();
    private final RemoveVisitor removeVisitor = new RemoveVisitor();

    // Since this is an in-memory data store, UniNodes and their values won't be persisted.
    // Therefore we should never require to solveUniNode values from storage. Hence it is
    // safe to pass a dummy data loader to the UniNodeFactory instance.
    private final DefaultUniNodeFactory nodeFactory = new DefaultUniNodeFactory(__ -> Optional.empty());

    private final Function<V, BytesValue> valueSerializer;
    private final Function<BytesValue, V> valueDeserializer;

    private UniNode root;

    public SimpleUniTrie(
            final Function<V, BytesValue> valueSerializer,
            final Function<BytesValue, V> valueDeserializer) {

        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
        this.root = NullUniNode.instance();
    }

    @Override
    public Optional<V> get(final K key) {
        checkNotNull(key);
        return root.accept(getVisitor, bytesToPath(key)).getValue().map(valueDeserializer);
    }

    @Override
    public Proof<V> getValueWithProof(final K key) {
        checkNotNull(key);
        final ProofVisitor proofVisitor = new ProofVisitor(root);
        final Optional<V> value = root.accept(proofVisitor, bytesToPath(key)).getValue().map(valueDeserializer);
        final List<BytesValue> proof = proofVisitor.getProof().stream()
                .map(UniNode::getEncoding).collect(Collectors.toList());
        return new Proof<>(value, proof);
    }

    @Override
    public void put(final K key, final V value) {
        checkNotNull(key);
        checkNotNull(value);
        this.root = root.accept(new PutVisitor(valueSerializer.apply(value), nodeFactory), bytesToPath(key));
    }

    @Override
    public void remove(final K key) {
        checkNotNull(key);
        this.root = root.accept(removeVisitor, bytesToPath(key));
    }

    @Override
    public Bytes32 getRootHash() {
        return root.getHash();
    }

    @Override
    public String toString() {
        //return getClass().getSimpleName() + "[" + getRootHash() + "]";
        return root.toString();
    }

    @Override
    public void commit(final NodeUpdater nodeUpdater) {
        // Nothing to do here
    }

    @Override
    public Map<Bytes32, V> entriesFrom(final Bytes32 startKeyHash, final int limit) {
        return UniTrieCollector.collectEntries(root, startKeyHash, limit, valueDeserializer);
    }

    @Override
    public void visitAll(final Consumer<BasicNode<V>> visitor) {
        root.accept(new AllUniNodesVisitor<>(valueDeserializer, visitor));
    }

    private BytesValue bytesToPath(final BytesValue key) {
        return PathEncoding.decodePath(key, key.size() * 8);
    }
}
