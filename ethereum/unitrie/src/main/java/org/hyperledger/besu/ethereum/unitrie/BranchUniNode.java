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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * An inner unitrie node, possibly with children. A leaf is comprised by an instance of this class
 * having two {@link NullUniNode} as children.
 *
 * @author ppedemon
 */
public class BranchUniNode extends LeafUniNode {

  private final UniNode leftChild;
  private final UniNode rightChild;

  private long childrenSize;



  BranchUniNode(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild,
      final long childrenSize) {

    super(path,valueWrapper,true);
    Preconditions.checkNotNull(path);
    Preconditions.checkNotNull(valueWrapper);
    Preconditions.checkNotNull(leftChild);
    Preconditions.checkNotNull(rightChild);

    this.leftChild = leftChild;
    this.rightChild = rightChild;
    //System.out.println(leftChild.toString());
    //System.out.println(rightChild.toString());
    this.childrenSize = childrenSize;
    encode(path,valueWrapper);
  }

  @Override
  public UniNode getLeftChild() {
    return leftChild;
  }

  @Override
  public UniNode getRightChild() {
    return rightChild;
  }

  @Override
  public long getChildrenSize() {
    if (childrenSize == -1) {
      if (isLeaf()) {
        childrenSize = 0;
      } else {
        childrenSize = leftChild.intrinsicSize() + rightChild.intrinsicSize();
      }
    }
    return childrenSize;
  }

  @Override
  boolean isLeaf() {
    return leftChild == NullUniNode.instance() && rightChild == NullUniNode.instance();
  }
}
