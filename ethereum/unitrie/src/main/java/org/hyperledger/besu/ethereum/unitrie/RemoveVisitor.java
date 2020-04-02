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

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Visitor implementing Remove operation in a unitrie. Removal can be recursive, in that case the
 * operation will remove the node and both subtrees.
 *
 * @author ppedemon
 */
public class RemoveVisitor implements UniPathVisitor {

  private final boolean recursiveRemove;
  private final UniNodeFactory nodeFactory;

  public RemoveVisitor(final UniNodeFactory nodeFactory) {
    this(false, nodeFactory);
  }

  RemoveVisitor(final boolean recursiveRemove, final UniNodeFactory nodeFactory) {
    this.recursiveRemove = recursiveRemove;
    this.nodeFactory = nodeFactory;
  }

  @Override
  public UniNode visit(final NullUniNode node, final BytesValue path) {
    return NullUniNode.instance();
  }

  @Override
  public UniNode visit(final AbstractUniNode node, final BytesValue path) {
    BytesValue nodePath = BytesValue.wrap(node.getPath());
    BytesValue commonPath = path.commonPrefix(nodePath);

    if (commonPath.size() == path.size() && commonPath.size() == nodePath.size()) {
      return recursiveRemove ? NullUniNode.instance() : node.removeValue(nodeFactory);
    }

    if (commonPath.size() < nodePath.size()) {
      return node;
    }

    byte pos = path.get(commonPath.size());
    BytesValue newPath = path.slice(commonPath.size() + 1);
    if (pos == 0) {
      return node.replaceChild(pos, node.getLeftChild().accept(this, newPath), nodeFactory);
    } else {
      return node.replaceChild(pos, node.getRightChild().accept(this, newPath), nodeFactory);
    }
  }
}
