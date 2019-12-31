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

import java.util.function.Consumer;

/**
 * Traverse a whole Unitrie, node by node.
 *
 * @author ppedemon
 */
public class AllUniNodesVisitor implements UniNodeVisitor {

  private final Consumer<UniNode> handler;

  public AllUniNodesVisitor(final Consumer<UniNode> handler) {
    this.handler = handler;
  }

  @Override
  public void visit(final NullUniNode node) {}

  @Override
  public void visit(final BranchUniNode node) {
    handler.accept(node);
    acceptAndUnload(node.getLeftChild());
    acceptAndUnload(node.getRightChild());
  }

  private void acceptAndUnload(final UniNode node) {
    node.accept(this);
    node.unload();
  }
}
