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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class CommitVisitor implements UniNodeVisitor {

  private final DataLoader loader;
  private final DataUpdater nodeUpdater;
  private final DataUpdater valueUpdater;

  CommitVisitor(
      final DataLoader loader, final DataUpdater nodeUpdater, final DataUpdater valueUpdater) {
    this.loader = loader;
    this.nodeUpdater = nodeUpdater;
    this.valueUpdater = valueUpdater;
  }

  @Override
  public void visit(final NullUniNode node) {}

  @Override
  public void visit(final AbstractUniNode node) {
    if (!node.isDirty()) {
      return;
    }

    if (node.getLeftChild().isDirty()) {
      node.getLeftChild().accept(this);
    }
    if (node.getRightChild().isDirty()) {
      node.getRightChild().accept(this);
    }

    maybeStoreNode(node);
  }

  private void maybeStoreNode(final UniNode node) {
    // If value is not embedded in node it must be explicitly stored
    if (node.getValueWrapper().isLong()) {
      node.getValue(loader)
          .flatMap(value -> node.getValueHash().map(hash -> new HashedValue(hash, value)))
          .ifPresentOrElse(
              hashedValue -> store(valueUpdater, hashedValue.hash, hashedValue.value),
              () -> {
                throw new IllegalStateException("Long valued node provides no hash or value");
              });
    }

    // If node is not embedded in its parent it must be explicitly stored
    if (node.isReferencedByHash()) {
      store(nodeUpdater, node.getHash(), node.getEncoding());
    }
  }

  private static void store(final DataUpdater updater, final byte[] hash, final byte[] value) {
    updater.store(Bytes32.wrap(hash), Bytes.wrap(value));
  }

  /** Dead simple (valueHash, value) pair. */
  private static class HashedValue {
    final byte[] hash;
    final byte[] value;

    HashedValue(final byte[] hash, final byte[] value) {
      this.hash = hash;
      this.value = value;
    }
  }
}
