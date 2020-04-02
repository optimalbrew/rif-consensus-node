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

import java.util.Optional;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Factory creating nodes backed up by a {@link DataLoader}. This includes nodes referenced by hash.
 *
 * @author ppedemon
 */
public class StoredUniNodeFactory implements UniNodeFactory {

  private final DataLoader loader;
  private final UniNodeEncoding encoding = new UniNodeEncoding();

  public StoredUniNodeFactory(final DataLoader loader) {
    this.loader = loader;
  }

  @Override
  public UniNode createLeaf(final byte[] path, final ValueWrapper valueWrapper) {
    return handleNewNode(new LeafUniNode(path, valueWrapper));
  }

  @Override
  public UniNode createBranch(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild) {

    return createBranch(path, valueWrapper, leftChild, rightChild, -1);
  }

  @Override
  public UniNode createBranch(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild,
      final int childrenSize) {

    if ((leftChild==NullUniNode.instance()) && (rightChild==NullUniNode.instance())) {
      return  handleNewNode(new LeafUniNode(path, valueWrapper));
    }
    return handleNewNode(
        new BranchUniNode(path, valueWrapper, leftChild, rightChild, childrenSize));
  }

  private UniNode handleNewNode(final UniNode node) {
    node.markDirty();
    return node;
  }

  /**
   * Retrieve a {@link UniNode} by hash using this instance loader.
   *
   * @param hash hash of node to retrieve
   * @return optional holding retrieved node, empty if there's no node associated to the given hash
   */
  public Optional<UniNode> retrieve(final Bytes32 hash) {
    return loader
        .load(hash)
        .map(
            value -> {
              UniNode node = decode(value);
              // recalculating the node.hash() is potentially expensive, so do it as an assertion
              assert (hash.equals(Bytes32.wrap(node.getHash())))
                  : "Node hash " + Bytes32.wrap(node.getHash()) + " not equal to expected " + hash;
              return node;
            });
  }

  /**
   * Decode the given value into a {@link UniNode}.
   *
   * @param value value to decode
   * @return decoded {@link UniNode} corresponding to the given value
   */
  public UniNode decode(final BytesValue value) {
    return encoding.decode(value, this);
  }

  /**
   * Decode the given value into a {@link UniNode}.
   *
   * @param value value to decode
   * @return decoded {@link UniNode} corresponding to the given value
   */
  public UniNode decode(final byte[] value) {
    return encoding.decode(BytesValue.wrap(value), this);
  }
}
