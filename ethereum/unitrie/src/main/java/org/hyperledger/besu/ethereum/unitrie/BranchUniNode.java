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
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.ethereum.unitrie.ints.VarInt;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

/**
 * An inner unitrie node, possibly with children. A leaf is comprised by an
 * instance of this class having two {@link NullUniNode} as children.
 *
 * @author ppedemon
 */
public final class BranchUniNode implements UniNode {

    private static final int MAX_INLINED_NODE_SIZE = 44;

    private static final UniNodeEncoding encodingHelper = new UniNodeEncoding();

    private final BytesValue path;
    private ValueWrapper valueWrapper;
    private final UniNode leftChild;
    private final UniNode rightChild;
    private VarInt childrenSize;

    private WeakReference<BytesValue> encoding;
    private SoftReference<Bytes32> hash;

    private final DataLoader loader;
    private final UniNodeFactory nodeFactory;

    private boolean dirty = false;

    BranchUniNode(
            final BytesValue path,
            final ValueWrapper valueWrapper,
            final DataLoader loader,
            final UniNodeFactory nodeFactory) {
        this(path, valueWrapper, NullUniNode.instance(), NullUniNode.instance(), null, loader, nodeFactory);
    }

    BranchUniNode(
            final BytesValue path,
            final ValueWrapper valueWrapper,
            final UniNode leftChild,
            final UniNode rightChild,
            final VarInt childrenSize,
            final DataLoader loader,
            final UniNodeFactory nodeFactory) {

        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(valueWrapper);
        Preconditions.checkNotNull(leftChild);
        Preconditions.checkNotNull(rightChild);
        Preconditions.checkNotNull(loader);
        Preconditions.checkNotNull(nodeFactory);

        this.path = path;
        this.valueWrapper = valueWrapper;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.childrenSize = childrenSize;
        this.loader = loader;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public BytesValue getPath() {
        return path;
    }

    @Override
    public ValueWrapper getValueWrapper() {
        return valueWrapper;
    }

    @Override
    public Optional<BytesValue> getValue() {
        return valueWrapper.solveValue(loader);
    }

    @Override
    public Optional<Bytes32> getValueHash() {
        return valueWrapper.getHash();
    }

    @Override
    public Optional<UInt24> getValueLength() {
        return valueWrapper.getLength();
    }

    @Override
    public String print(final int indent) {
        return String.format("%s%s%s%s",
                Strings.repeat(" ", indent),
                String.format("(key = %s (%d), cs = %s, val = %s)", path, path.size(), childrenSize, valueWrapper),
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
    public UniNode getLeftChild() {
        return leftChild;
    }

    @Override
    public UniNode getRightChild() {
        return rightChild;
    }

    @Override
    public VarInt getChildrenSize() {
        if (Objects.isNull(childrenSize)) {
            if (isLeaf()) {
                childrenSize = VarInt.ZERO;
            } else {
                childrenSize = new VarInt(leftChild.intrinsicSize() + rightChild.intrinsicSize());
            }
        }
        return childrenSize;
    }

    @Override
    public long intrinsicSize() {
        int valueSize = valueWrapper.isLong()? valueWrapper.getLength().map(UInt24::toInt).orElse(0) : 0;
        return valueSize + getChildrenSize().getValue() + getEncoding().size();
    }

    @Override
    public BytesValue getEncoding() {
        if (Objects.nonNull(encoding)) {
            BytesValue v = encoding.get();
            if (Objects.nonNull(v)) {
                return v;
            }
        }

        BytesValue v = encodingHelper.encode(this);
        encoding = new WeakReference<>(v);
        return v;
    }

    @Override
    public Bytes32 getHash() {
        if (Objects.nonNull(hash)) {
            Bytes32 h = hash.get();
            if (h != null) {
                return h;
            }
        }

        Bytes32 h = Hash.keccak256(getEncoding());
        hash = new SoftReference<>(h);
        return h;
    }

    @Override
    public boolean isReferencedByHash() {
        return !isLeaf() || getEncoding().size() > MAX_INLINED_NODE_SIZE;
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

    UniNode removeValue() {
        // By removing this node's value we might have a chance to coalesce
        return coalesce(nodeFactory.createBranch(path, ValueWrapper.EMPTY, leftChild, rightChild), nodeFactory);
    }

    UniNode replaceValue(final BytesValue newValue) {
        Preconditions.checkNotNull(newValue, "Can't call replaceValue with null, call removeValue instead");
        if (valueWrapper.wrappedValueIs(newValue)) {
            return this;
        }
        return nodeFactory.createBranch(path, ValueWrapper.fromValue(newValue), leftChild, rightChild);
    }

    UniNode replacePath(final BytesValue newPath) {
        if (newPath.equals(path)) {
            return this;
        }
        return nodeFactory.createBranch(newPath, valueWrapper, leftChild, rightChild);
    }

    UniNode replaceChild(final byte pos, final UniNode newChild) {
        if (pos == 0) {
            if (newChild == leftChild) {
                return this;
            }
            return coalesce(nodeFactory.createBranch(path, valueWrapper, newChild, rightChild), nodeFactory);
        } else {
            if (newChild == rightChild) {
                return this;
            }
            return coalesce(nodeFactory.createBranch(path, valueWrapper, leftChild, newChild), nodeFactory);
        }
    }

    /**
     * Possibly coalesce a node. Rules are:
     *
     *   - Node has a value: keep node as it is.
     *   - Node has no value and two children: keep node as it is.
     *   - Node has no value and no children: turn it into a {@link NullUniNode}.
     *   - Node has no value and a single child: replace it with the child (enlarging its path with
     *     the parent path and pos bit).
     *
     * So, for a branch with no value and a left child with path p, return the left child with path 0:p.
     * Conversely, if the branch has no value and a right child with path p, return the right child with path 1:p.
     *
     * @param node         node to possible coalesce
     * @param nodeFactory  node factory used to create a new node in case of coalescing
     * @return  original node, or coalesced one.
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

        return nodeFactory.createBranch(
                node.getPath().concat(BytesValue.of(pos)).concat(child.getPath()),
                child.getValueWrapper(),
                child.getLeftChild(),
                child.getRightChild());
    }
}
