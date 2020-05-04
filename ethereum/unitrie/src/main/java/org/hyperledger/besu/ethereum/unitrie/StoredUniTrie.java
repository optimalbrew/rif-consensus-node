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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hyperledger.besu.ethereum.trie.NodeUpdater;
import org.hyperledger.besu.ethereum.trie.Proof;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Unitrie backed by some key value storage.
 *
 * @param <K> key type
 * @param <V> value type
 * @author ppedemon
 */
public class StoredUniTrie<K extends Bytes, V> implements UniTrie<K, V> {

  private final DataLoader loader;
  private final StoredUniNodeFactory nodeFactory;

  private final GetVisitor getVisitor;
  private final RemoveVisitor removeVisitor;
  private final RemoveVisitor recursiveRemoveVisitor;

  private final Function<V, byte[]> valueSerializer;
  private final Function<byte[], V> valueDeserializer;

  private UniNode root;

  public StoredUniTrie(
      final DataLoader loader,
      final Function<V, Bytes> valueSerializer,
      final Function<Bytes, V> valueDeserializer) {
    this(loader, NULL_UNINODE_HASH, valueSerializer, valueDeserializer);
  }

  public StoredUniTrie(
      final DataLoader loader,
      final Bytes32 rootHash,
      final Function<V, Bytes> valueSerializer,
      final Function<Bytes, V> valueDeserializer) {

    this.loader = loader;
    this.nodeFactory = new StoredUniNodeFactory(loader);

    this.valueSerializer = valueSerializer.andThen(Bytes::toArrayUnsafe);
    this.valueDeserializer = valueDeserializer.compose(Bytes::of);

    this.getVisitor = new GetVisitor();
    this.removeVisitor = new RemoveVisitor(nodeFactory);
    this.recursiveRemoveVisitor = new RemoveVisitor(true, nodeFactory);

    this.root =
        rootHash.equals(NULL_UNINODE_HASH)
            ? NullUniNode.instance()
            : new StoredUniNode(rootHash.toArrayUnsafe(), nodeFactory);
  }

  @Override
  public Optional<V> get(final K key) {
    checkNotNull(key);
    return root.accept(getVisitor, bytesToPath(key)).getValue(loader).map(valueDeserializer);
  }

  @Override
  public Bytes32 getHash(final K key) {
    checkNotNull(key);
    return Bytes32.wrap(root.accept(getVisitor, bytesToPath(key)).getHash());
  }

  @Override
  public Proof<V> getValueWithProof(final K key) {
    checkNotNull(key);
    final ProofVisitor proofVisitor = new ProofVisitor(root);
    final Optional<V> value =
        root.accept(proofVisitor, bytesToPath(key)).getValue(loader).map(valueDeserializer);
    final List<Bytes> proof =
        proofVisitor.getProof().stream()
            .map(UniNode::getEncoding)
            .map(Bytes::of)
            .collect(Collectors.toList());
    return new Proof<>(value, proof);
  }

  @Override
  public Optional<Integer> getValueLength(final K key) {
    checkNotNull(key);
    return root.accept(getVisitor, bytesToPath(key))
        .getValueWrapper()
        .getLength();
  }

  @Override
  public Optional<Bytes32> getValueHash(final K key) {
    checkNotNull(key);
    return root.accept(getVisitor, bytesToPath(key))
        .getValueWrapper()
        .getHash()
        .map(Bytes32::wrap);
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
  public void commit(final NodeUpdater nodeUpdater, final NodeUpdater valueUpdater) {
    final CommitVisitor commitVisitor =
        new CommitVisitor(loader, nodeUpdater::store, valueUpdater::store);
    root.accept(commitVisitor);

    final byte[] rootHash = root.getHash();
    final Bytes32 h = Bytes32.wrap(rootHash);

    // Make sure root node was stored
    if (root.isDirty() && !root.isReferencedByHash()) {
      nodeUpdater.store(h, Bytes.of(root.getEncoding()));
    }

    // Reset root so dirty nodes can be garbage collected
    this.root =
        h.equals(NULL_UNINODE_HASH)
            ? NullUniNode.instance()
            : new StoredUniNode(rootHash, nodeFactory);
  }

  @Override
  public void visitAll(final Consumer<UniNode> visitor) {
    root.accept(new AllUniNodesVisitor(visitor));
  }

  @Override
  public Bytes32 getRootHash() {
    return Bytes32.wrap(root.getHash());
  }

  @Override
  public boolean isLeaf(final K key) {
    checkNotNull(key);
    UniNode node = root.accept(getVisitor, bytesToPath(key));
    return node != NullUniNode.instance()
        && node.getLeftChild() == NullUniNode.instance()
        && node.getRightChild() == NullUniNode.instance();
  }

  @Override
  public String toString() {
    //return getClass().getSimpleName() + "[" + getRootHash() + "]";
    return root.toString();
  }

  private Bytes bytesToPath(final Bytes key) {
    return PathEncoding.decodePath(key, key.size() * 8);
  }
}
