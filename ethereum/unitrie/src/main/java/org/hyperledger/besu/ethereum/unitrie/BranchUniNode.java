package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
    private final BytesValue value;

    private final UniNode leftChild;
    private final UniNode rightChild;

    private final UniNodeFactory nodeFactory;

    BranchUniNode(final BytesValue path, final BytesValue value, final UniNodeFactory nodeFactory) {
        this(path, value, NullUniNode.instance(), NullUniNode.instance(), nodeFactory);
    }

    BranchUniNode(
            final BytesValue path,
            final BytesValue value,
            final UniNode leftChild,
            final UniNode rightChild,
            final UniNodeFactory nodeFactory) {

        Preconditions.checkNotNull(leftChild, "left child is null");
        Preconditions.checkNotNull(rightChild, "right child is null");

        this.path = path;
        this.value = value;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public BytesValue getPath() {
        return path;
    }

    @Override
    public Optional<BytesValue> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public String print(final int indent) {
        return String.format("%s%s%s%s",
                Strings.repeat(" ", indent),
                String.format("(key = %s, val = %s)", path, value),
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
        return coalesce(nodeFactory.createBranch(path, null, leftChild, rightChild), nodeFactory);
    }

    UniNode replaceValue(final BytesValue newValue) {
        if (newValue.equals(this.value)) {
            return this;
        }
        return nodeFactory.createBranch(path, newValue, leftChild, rightChild);
    }

    UniNode replacePath(final BytesValue newPath) {
        if (newPath.equals(path)) {
            return this;
        }
        return nodeFactory.createBranch(newPath, value, leftChild, rightChild);
    }

    UniNode replaceChild(final byte pos, final UniNode newChild) {
        if (pos == 0) {
            if (newChild == leftChild) {
                return this;
            }
            return coalesce(nodeFactory.createBranch(path, value, newChild, rightChild), nodeFactory);
        } else {
            if (newChild == rightChild) {
                return this;
            }
            return coalesce(nodeFactory.createBranch(path, value, leftChild, newChild), nodeFactory);
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
        if (node.getValue().isPresent()) {
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
                child.getValue().orElse(null),
                child.getLeftChild(),
                child.getRightChild());
    }
}
