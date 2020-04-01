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
public class BranchUniNode implements UniNode {

  private static final int MAX_INLINED_NODE_SIZE = 44;

  private static final UniNodeEncoding encodingHelper = new UniNodeEncoding();
  private ValueWrapper longValueWrapper;
  private WeakReference<byte[]> pathWeakReference;
  private final UniNode leftChild;
  private final UniNode rightChild;

  private long childrenSize;
  private byte[] encoding;

  private boolean dirty = false;

  BranchUniNode(final byte[] path, final ValueWrapper valueWrapper) {
    this(path, valueWrapper, NullUniNode.instance(), NullUniNode.instance(), -1);
  }

  BranchUniNode(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild,
      final long childrenSize) {

    Preconditions.checkNotNull(path);
    Preconditions.checkNotNull(valueWrapper);
    Preconditions.checkNotNull(leftChild);
    Preconditions.checkNotNull(rightChild);


    if (valueWrapper.isLong())
        this.longValueWrapper = valueWrapper;
    this.pathWeakReference = new  WeakReference<>(path);
    this.leftChild = leftChild;
    this.rightChild = rightChild;
    //System.out.println(leftChild.toString());
    //System.out.println(rightChild.toString());
    this.childrenSize = childrenSize;
    encode(path,valueWrapper);
  }
  public void clearWeakReferences() {
      //longValueWrapper.clear();
      pathWeakReference.clear();
  }
  @Override
  public byte[] getPath() {
    if (pathWeakReference != null) {
      byte[] v = pathWeakReference.get();
      if (v != null) {
        return v;
      }
    }
    byte[] path=encodingHelper.decodePathFromFullEncoding(ByteBuffer.wrap(encoding));
    pathWeakReference =new WeakReference<>(path);

    return path;
  }

  @Override
  public ValueWrapper getValueWrapper() {
    if (longValueWrapper!= null) {
        return longValueWrapper;
    }

    ValueWrapper valueWrapper = encodingHelper.decodeValueWrapperFromFullEncoding(ByteBuffer.wrap(encoding));
    return valueWrapper;
  }

  @Override
  public Optional<byte[]> getValue(final DataLoader loader) {
    return getValueWrapper().solveValue(loader);
  }

  @Override
  public Optional<byte[]> getValueHash() {
    return getValueWrapper().getHash();
  }

  @Override
  public Optional<Integer> getValueLength() {
    return getValueWrapper().getLength();
  }

  @Override
  public String print(final int indent) {
    BytesValue path = PathEncoding.encodePath(BytesValue.wrap(getPath()));
    long pathLength = getPath().length;
    ValueWrapper vr = getValueWrapper();
    return String.format(
        "%s%s%s%s",
        Strings.repeat(" ", indent),
        String.format(
            "(key = %s (%d), cs = %d, val = %s)",
            path,
            pathLength,
            childrenSize,vr
                ),
        String.format("\n%s", leftChild.print(indent + 2)),
        String.format("\n%s", rightChild.print(indent + 2)));
  }

  @Override
  public String toString() {
    return print(0);
  }

  @Override
  public UniNode accept(final UniPathVisitor visitor, final BytesValue path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final UniNodeVisitor visitor) {
    visitor.visit(this);
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
  public long intrinsicSize() {
    ValueWrapper valueWrapper = getValueWrapper();
    int valueSize =
        valueWrapper.isLong() ? valueWrapper.getLength().orElse(0) : 0;
    return valueSize + getChildrenSize() + getEncoding().length;
  }

  public ValueWrapper unWrapValue() {
    return encodingHelper.decodeValueWrapperFromFullEncoding(ByteBuffer.wrap(encoding));
  }

  @Override
  public byte[] getEncoding() {
    return encoding;
  }

  public byte[] encode(final byte[] path, final ValueWrapper valueWrapper) {
    ExpandedUniNode e = new ExpandedUniNode(path,valueWrapper,leftChild,rightChild,childrenSize);
    byte[] rawEnc = encodingHelper.encode(e).getArrayUnsafe();
    this.encoding =rawEnc;
    return rawEnc;
  }

  @Override
  public byte[] getHash() {
    //if (Objects.isNull(hash)) {
      BytesValue encoding = BytesValue.of(getEncoding());
      byte[] hash = Hash.keccak256(encoding).getArrayUnsafe();

    return hash;
  }

  @Override
  public boolean isReferencedByHash() {
    return !isLeaf() || getEncoding().length > MAX_INLINED_NODE_SIZE;
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }

  boolean isLeaf() {
    return leftChild == NullUniNode.instance() && rightChild == NullUniNode.instance();
  }

  UniNode removeValue(final UniNodeFactory nodeFactory) {
    // By removing this node's value we might have a chance to coalesce
    return coalesce(
        nodeFactory.createBranch(getPath(), ValueWrapper.EMPTY, leftChild, rightChild), nodeFactory);
  }

  UniNode replaceValue(final byte[] newValue, final UniNodeFactory nodeFactory) {
    Preconditions.checkNotNull(
        newValue, "Can't call replaceValue with null, call removeValue instead");
    if (getValueWrapper().wrappedValueIs(newValue)) {
      return this;
    }
    return nodeFactory.createBranch(getPath(), ValueWrapper.fromValue(newValue), leftChild, rightChild);
  }

  UniNode replacePath(final byte[] newPath, final UniNodeFactory nodeFactory) {
    if (Arrays.equals(newPath, getPath())) {
      return this;
    }
    return nodeFactory.createBranch(newPath, getValueWrapper(), leftChild, rightChild);
  }

  UniNode replaceChild(final byte pos, final UniNode newChild, final UniNodeFactory nodeFactory) {
    if (pos == 0) {
      if (newChild == leftChild) {
        return this;
      }
      return coalesce(
          nodeFactory.createBranch(getPath(), getValueWrapper(), newChild, rightChild), nodeFactory);
    } else {
      if (newChild == rightChild) {
        return this;
      }
      return coalesce(
          nodeFactory.createBranch(getPath(), getValueWrapper(), leftChild, newChild), nodeFactory);
    }
  }

  /**
   * Possibly coalesce a node. Rules are:
   *
   * <ul>
   *   <li>Node has a value: keep node as it is.
   *   <li>Node has no value and two children: keep node as it is.
   *   <li>Node has no value and no children: turn it into a {@link NullUniNode}.
   *   <li>Node has no value and a single child: replace it with the child (enlarging its path with
   *       the parent path and pos bit).
   * </ul>
   *
   * <p>So, for a branch with path p, no value and a left child with path q, return the left child
   * with path p:0:q. Conversely, if the branch has no value and a right child with path q, return
   * the right child with path p:1:q.
   *
   * @param node node to possible coalesce
   * @param nodeFactory node factory used to create a new node in case of coalescing
   * @return original node, or coalesced one.
   */
  private static UniNode coalesce(final UniNode node, final UniNodeFactory nodeFactory) {
    if (!node.getValueWrapper().isEmpty()) {
      return node;
    }

    boolean hasLeftChild = node.getLeftChild() != NullUniNode.instance();
    boolean hasRightChild = node.getRightChild() != NullUniNode.instance();

    // No value, both children: this is a branch node with no associated value, must keep
    if (hasLeftChild && hasRightChild) {
      return node;
    }

    // No value, no children: this is the NullUniNode
    if (!hasLeftChild && !hasRightChild) {
      return NullUniNode.instance();
    }

    // No value, only one child: we can actually coalesce
    byte pos;
    UniNode child;
    if (hasLeftChild) {
      pos = 0;
      child = node.getLeftChild();
    } else {
      pos = 1;
      child = node.getRightChild();
    }

    byte[] nodePath = node.getPath();
    byte[] childPath = child.getPath();
    byte[] newPath = Arrays.copyOf(nodePath, nodePath.length + 1 + childPath.length);
    newPath[nodePath.length] = pos;
    System.arraycopy(childPath, 0, newPath, nodePath.length + 1, childPath.length);

    return nodeFactory.createBranch(
        newPath,
        child.getValueWrapper(),
        child.getLeftChild(),
        child.getRightChild());
  }
}
