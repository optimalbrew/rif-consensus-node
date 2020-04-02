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

import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implementing a {@link GetVisitor} that in addition records the not embedded nodes visited
 * through the path.
 *
 * <p>The accumulated nodes can be regarded as a proof that the queried node (if found) genuinely
 * belongs to the Unitrie.
 *
 * @author ppedemon
 */
public class ProofVisitor extends GetVisitor {

  private final UniNode rootNode;
  private final List<UniNode> proof = new ArrayList<>();

  ProofVisitor(final UniNode rootNode) {
    this.rootNode = rootNode;
  }

  @Override
  public UniNode visit(final LeafUniNode branchNode, final BytesValue path) {
    maybeTrackNode(branchNode);
    return super.visit(branchNode, path);
  }

  @Override
  public UniNode visit(final NullUniNode nullNode, final BytesValue path) {
    return super.visit(nullNode, path);
  }

  public List<UniNode> getProof() {
    return proof;
  }

  private void maybeTrackNode(final UniNode node) {
    if (node.equals(rootNode) || node.isReferencedByHash()) {
      proof.add(node);
    }
  }
}
