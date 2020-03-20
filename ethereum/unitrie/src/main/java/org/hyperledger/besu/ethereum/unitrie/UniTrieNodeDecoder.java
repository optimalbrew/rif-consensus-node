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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Streams;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

public class UniTrieNodeDecoder {

  private final StoredUniNodeFactory nodeFactory;

  public UniTrieNodeDecoder(final DataLoader loader) {
    nodeFactory = new StoredUniNodeFactory(loader);
  }

  /**
   * Do a BFS from a given UniNode, returning the found nodes in order.
   *
   * @param loader data loader
   * @param rootHash hash of BFS root node
   * @param maxDepth cutoff depth
   * @return nodes visited by the BFS traversal, in order
   */
  public static Stream<UniNode> breadthFirstDecoder(
      final DataLoader loader, final Bytes32 rootHash, final int maxDepth) {

    checkArgument(maxDepth >= 0);
    return Streams.stream(new UniTrieNodeDecoder.BreadthFirstIterator(loader, rootHash, maxDepth));
  }

  /**
   * Do a BFS from a given UniNode, returning the found nodes in order.
   *
   * @param loader data loader
   * @param rootHash hash of BFS root node
   * @return nodes visited by the BFS traversal, in order
   */
  public static Stream<UniNode> breadthFirstDecoder(
      final DataLoader loader, final Bytes32 rootHash) {
    return breadthFirstDecoder(loader, rootHash, Integer.MAX_VALUE);
  }

  /**
   * Decode a Uninode.
   *
   * @param value encoded UniBode
   * @return decoded {@link UniNode}
   */
  public UniNode decode(final BytesValue value) {
    return nodeFactory.decode(value);
  }

  /**
   * Flattens this UniNode and all of its embedded descendants.
   *
   * @param value bytes of the UniNode to be decoded
   * @return list of UniNodes and references embedded in the given encoded value.
   */
  public List<UniNode> decodeNodes(final BytesValue value) {
    UniNode node = decode(value);
    List<UniNode> nodes = new ArrayList<>();
    nodes.add(node);

    final Deque<UniNode> toProcess = new ArrayDeque<>();
    toProcess.addLast(node.getLeftChild());
    toProcess.addLast(node.getRightChild());

    while (!toProcess.isEmpty()) {
      final UniNode currentNode = toProcess.removeFirst();
      if (Objects.equals(currentNode, NullUniNode.instance())) {
        continue;
      }
      nodes.add(currentNode);

      if (!currentNode.isReferencedByHash()) {
        // If current node is inlined, that means we can process its children
        toProcess.addLast(currentNode.getLeftChild());
        toProcess.addLast(currentNode.getRightChild());
      }
    }

    return nodes;
  }

  private static class BreadthFirstIterator implements Iterator<UniNode> {

    private final int maxDepth;
    private final StoredUniNodeFactory nodeFactory;
    private final Deque<UniNode> currentNodes = new ArrayDeque<>();
    private final Deque<UniNode> nextNodes = new ArrayDeque<>();
    private int currentDepth = 0;

    BreadthFirstIterator(final DataLoader loader, final Bytes32 rootHash, final int maxDepth) {
      this.maxDepth = maxDepth;
      this.nodeFactory = new StoredUniNodeFactory(loader);
      loader.load(rootHash).map(nodeFactory::decode).ifPresent(currentNodes::addLast);
    }

    @Override
    public boolean hasNext() {
      return !currentNodes.isEmpty() && currentDepth <= maxDepth;
    }

    @Override
    public UniNode next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      final UniNode nextNode = currentNodes.removeFirst();

      final Deque<UniNode> children = new ArrayDeque<>();
      children.addLast(nextNode.getLeftChild());
      children.addLast(nextNode.getRightChild());

      while (!children.isEmpty()) {
        UniNode child = children.removeFirst();
        if (Objects.equals(child, NullUniNode.instance())) {
          continue;
        }
        if (child.isReferencedByHash()) {
          // Retrieve hash-referenced child
          final Optional<UniNode> maybeChildNode =
              nodeFactory.retrieve(Bytes32.wrap(child.getHash()));
          if (maybeChildNode.isEmpty()) {
            continue;
          }
          child = maybeChildNode.get();
        }
        nextNodes.addLast(child);
      }

      // Set up next level
      if (currentNodes.isEmpty()) {
        currentDepth += 1;
        currentNodes.addAll(nextNodes);
        nextNodes.clear();
      }

      return nextNode;
    }
  }
}
