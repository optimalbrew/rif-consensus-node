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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import org.hyperledger.besu.ethereum.trie.NodeUpdater;
import org.hyperledger.besu.ethereum.trie.Proof;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simple in-memory Unitrie.
 *
 * @param <K> key type
 * @param <V> value type
 * @author ppedemon
 */
public class SimpleUniTrie<K extends BytesValue, V> implements UniTrie<K, V> {

  private final GetVisitor getVisitor = new GetVisitor();
  private final RemoveVisitor removeVisitor = new RemoveVisitor();
  private final RemoveVisitor recursiveRemoveVisitor = new RemoveVisitor(true);

  // Since this is an in-memory data store, UniNodes and their values won't be persisted.
  // Therefore we should never require to solveUniNode values from storage. Hence it is
  // safe to pass a dummy data loader to the UniNodeFactory instance.
  private final DefaultUniNodeFactory nodeFactory =
      new DefaultUniNodeFactory(__ -> Optional.empty());

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

  // For creating a sub unitrie only
  private SimpleUniTrie(
      final UniNode root,
      final Function<V, BytesValue> valueSerializer,
      final Function<BytesValue, V> valueDeserializer) {

    this.root = root;
    this.valueSerializer = valueSerializer;
    this.valueDeserializer = valueDeserializer;
  }

  @Override
  public UniTrie<K, V> getSubUniTrie(final K key) {
    checkNotNull(key);
    UniNode node = root.accept(getVisitor, bytesToPath(key));
    return new SimpleUniTrie<>(node, valueSerializer, valueDeserializer);
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
    final Optional<V> value =
        root.accept(proofVisitor, bytesToPath(key)).getValue().map(valueDeserializer);
    final List<BytesValue> proof =
        proofVisitor.getProof().stream().map(UniNode::getEncoding).collect(Collectors.toList());
    return new Proof<>(value, proof);
  }

  @Override
  public void put(final K key, final V value) {
    checkNotNull(key);
    checkNotNull(value);
    this.root =
        root.accept(new PutVisitor(valueSerializer.apply(value), nodeFactory), bytesToPath(key));
  }

  @Override
  public void remove(final K key) {
    checkNotNull(key);
    this.root = root.accept(removeVisitor, bytesToPath(key));
  }

  @Override
  public void removeRecursive(final K key) {
    checkNotNull(key);
    this.root = root.accept(recursiveRemoveVisitor, bytesToPath(key));
  }

  @Override
  public Bytes32 getRootHash() {
    return root.getHash();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getRootHash() + "]";
  }

  @Override
  public void commit(final NodeUpdater nodeUpdater) {
    // Nothing to do here
  }

  @Override
  public void visitAll(final Consumer<UniNode> visitor) {
    root.accept(new AllUniNodesVisitor(visitor));
  }

  @Override
  public Map<BytesValue, V> entriesFrom(final BytesValue startPath, final int limit) {
    return UniTrieCollector.collectEntries(root, startPath, limit, valueDeserializer);
  }

  private BytesValue bytesToPath(final BytesValue key) {
    return PathEncoding.decodePath(key, key.size() * 8);
  }
}
