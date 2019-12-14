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
import static org.hyperledger.besu.ethereum.unitrie.UniNodeEncoding.NULL_UNINODE_HASH;

/**
 * Unitrie backed by some key value storage.
 *
 * @param <K>  key type
 * @param <V>  value type
 * @author ppedemon
 */
public class StoredUnitrie<K extends BytesValue, V> implements MerklePatriciaTrie<K, V> {

    private final GetVisitor getVisitor = new GetVisitor();
    private final RemoveVisitor removeVisitor = new RemoveVisitor();

    private final StoredUniNodeFactory nodeFactory;
    private final Function<V, BytesValue> valueSerializer;
    private final Function<BytesValue, V> valueDeserializer;

    private UniNode root;

    public StoredUnitrie(
            final DataLoader loader,
            final Function<V, BytesValue> valueSerializer,
            final Function<BytesValue, V> valueDeserializer) {
        this(loader, NULL_UNINODE_HASH, valueSerializer, valueDeserializer);
    }

    public StoredUnitrie(
            final DataLoader loader,
            final Bytes32 rootHash,
            final Function<V, BytesValue> valueSerializer,
            final Function<BytesValue, V> valueDeserializer) {

        this.valueSerializer = valueSerializer;
        this.valueDeserializer = valueDeserializer;
        this.nodeFactory = new StoredUniNodeFactory(loader);
        this.root = rootHash.equals(NULL_UNINODE_HASH) ?
                NullUniNode.instance() : new StoredUniNode(rootHash, nodeFactory);
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
    public void commit(final NodeUpdater nodeUpdater) {
        final CommitVisitor commitVisitor = new CommitVisitor(nodeUpdater::store);
        root.accept(commitVisitor);
        // Make sure root node was stored
        if (root.isDirty() && !root.isReferencedByHash()) {
            nodeUpdater.store(root.getHash(), root.getEncoding());
        }
        // Reset root so dirty nodes can be garbage collected
        final Bytes32 rootHash = root.getHash();
        this.root = rootHash.equals(NULL_UNINODE_HASH) ?
                NullUniNode.instance() : new StoredUniNode(rootHash, nodeFactory);
    }

    @Override
    public Map<Bytes32, V> entriesFrom(final Bytes32 startKeyHash, final int limit) {
        return UniTrieCollector.collectEntries(root, startKeyHash, limit, valueDeserializer);
    }

    @Override
    public void visitAll(final Consumer<BasicNode<V>> visitor) {
        root.accept(new AllUniNodesVisitor<>(valueDeserializer, visitor));
    }

    @Override
    public Bytes32 getRootHash() {
        return root.getHash();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getRootHash() + "]";
    }

    private BytesValue bytesToPath(final BytesValue key) {
        return PathEncoding.decodePath(key, key.size() * 8);
    }
}
