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

/**
 * Vanilla implementation of a {@link UniNodeFactory}.
 *
 * @author ppedemon
 */
public class DefaultUniNodeFactory implements UniNodeFactory {

  @Override
  public UniNode createLeaf(final byte[] path, final ValueWrapper valueWrapper) {
    return new LeafUniNode(path, valueWrapper);
  }

  @Override
  public UniNode createBranch(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild) {

    if ((leftChild==NullUniNode.instance()) && (rightChild==NullUniNode.instance())) {
      return  new LeafUniNode(path, valueWrapper);
    }
    return new BranchUniNode(path, valueWrapper, leftChild, rightChild, -1);
  }

  @Override
  public UniNode createBranch(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild,
      final int childrenSize) {
    return new BranchUniNode(path, valueWrapper, leftChild, rightChild, childrenSize);
  }
}
