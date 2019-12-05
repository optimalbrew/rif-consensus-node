package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;

/**
 * An inner unitrie node, possibly with children. A leaf is comprised by an
 * instance of this class having two {@link NullUniNode} as children.
 *
 * @author ppedemon
 */
public final class BranchUniNode implements UniNode {

    private final BytesValue path;
    private ValueWrapper valueWrapper;
    private final UniNode leftChild;
    private final UniNode rightChild;

    private final MerkleStorage storage;
    private final UniNodeFactory nodeFactory;

    BranchUniNode(
            final BytesValue path,
            final ValueWrapper valueWrapper,
            final MerkleStorage storage,
            final UniNodeFactory nodeFactory) {
        this(path, valueWrapper, NullUniNode.instance(), NullUniNode.instance(), storage, nodeFactory);
    }

    BranchUniNode(
            final BytesValue path,
            final ValueWrapper valueWrapper,
            final UniNode leftChild,
            final UniNode rightChild,
            final MerkleStorage storage,
            final UniNodeFactory nodeFactory) {

        Preconditions.checkNotNull(path, "path can't be null");
        Preconditions.checkNotNull(valueWrapper, "value wrapper can't be null");
        Preconditions.checkNotNull(leftChild, "left child is null");
        Preconditions.checkNotNull(rightChild, "right child is null");
        Preconditions.checkNotNull(storage, "storage can;t be null");
        Preconditions.checkNotNull(nodeFactory, "node factory can't be null");

        this.path = path;
        this.valueWrapper = valueWrapper;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.storage = storage;
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
        return valueWrapper.solveValue(storage);
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
                String.format("(key = %s (%d), val = %s)", path, path.size(), valueWrapper),
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
