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

import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

public class CommitVisitor implements UniNodeVisitor {

  private final DataUpdater nodeUpdater;
  private final DataUpdater valueUpdater;

  CommitVisitor(final DataUpdater nodeUpdater, final DataUpdater valueUpdater) {
    this.nodeUpdater = nodeUpdater;
    this.valueUpdater = valueUpdater;
  }

  @Override
  public void visit(final NullUniNode node) {}

  @Override
  public void visit(final BranchUniNode node) {
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

  private void maybeStoreNode(final BranchUniNode node) {
    // If value is not embedded in node it must be explicitly stored
    if (node.getValueWrapper().isLong()) {
      node.getValue()
          .flatMap(value -> node.getValueHash().map(hash -> new HashedValue(hash, value)))
          .ifPresentOrElse(
              hashedValue -> valueUpdater.store(hashedValue.hash, hashedValue.value),
              () -> {
                throw new IllegalStateException("Long valued node provides no hash or value");
              });
    }

    // If node is not embedded in its parent it must be explicitly stored
    if (node.isReferencedByHash()) {
      nodeUpdater.store(node.getHash(), node.getEncoding());
    }
  }

  /** Dead simple (valueHash, value) pair. */
  private static class HashedValue {
    final Bytes32 hash;
    final BytesValue value;

    HashedValue(final Bytes32 hash, final BytesValue value) {
      this.hash = hash;
      this.value = value;
    }
  }
}
