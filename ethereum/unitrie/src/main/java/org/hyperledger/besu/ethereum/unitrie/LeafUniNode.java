package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public class LeafUniNode implements UniNode {

    private static final int MAX_INLINED_NODE_SIZE = 44;

    private static final UniNodeEncoding encodingHelper = new UniNodeEncoding();
    private ValueWrapper longValueWrapper;
    private WeakReference<byte[]> pathWeakReference;
    private byte[] encoding;

    private boolean dirty = false;

    // Do not encode on creation.
    LeafUniNode(
            final byte[] path,
            final ValueWrapper valueWrapper,final boolean internal) {

        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(valueWrapper);

        if (valueWrapper.isLong())
            this.longValueWrapper = valueWrapper;
        this.pathWeakReference = new  WeakReference<>(path);
    }

     LeafUniNode(
            final byte[] path,
            final ValueWrapper valueWrapper) {

        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(valueWrapper);

        if (valueWrapper.isLong())
            this.longValueWrapper = valueWrapper;
        this.pathWeakReference = new  WeakReference<>(path);
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
                        getChildrenSize(),vr
                ),
                String.format("\n%s", getLeftChild().print(indent + 2)),
                String.format("\n%s", getRightChild().print(indent + 2)));
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
        return NullUniNode.instance();
    }

    @Override
    public UniNode getRightChild() {
        return NullUniNode.instance();
    }

    @Override
    public long getChildrenSize() {
         return 0;
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
        ExpandedUniNode e = new ExpandedUniNode(path,valueWrapper,getLeftChild(),getRightChild(),getChildrenSize());
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
        return true;
    }

    UniNode removeValue(final UniNodeFactory nodeFactory) {
        // By removing this node's value we might have a chance to coalesce
        return coalesce(
                nodeFactory.createBranch(getPath(), ValueWrapper.EMPTY, getLeftChild(), getRightChild()), nodeFactory);
    }

    UniNode replaceValue(final byte[] newValue, final UniNodeFactory nodeFactory) {
        Preconditions.checkNotNull(
                newValue, "Can't call replaceValue with null, call removeValue instead");
        if (getValueWrapper().wrappedValueIs(newValue)) {
            return this;
        }
        return nodeFactory.createBranch(getPath(), ValueWrapper.fromValue(newValue), getLeftChild(), getRightChild());
    }

    UniNode replacePath(final byte[] newPath, final UniNodeFactory nodeFactory) {
        if (Arrays.equals(newPath, getPath())) {
            return this;
        }
        return nodeFactory.createBranch(newPath, getValueWrapper(), getLeftChild(), getRightChild());
    }

    UniNode replaceChild(final byte pos, final UniNode newChild, final UniNodeFactory nodeFactory) {
        if (pos == 0) {
            if (newChild == getLeftChild()) {
                return this;
            }
            return coalesce(
                    nodeFactory.createBranch(getPath(), getValueWrapper(), newChild, getRightChild()), nodeFactory);
        } else {
            if (newChild == getRightChild()) {
                return this;
            }
            return coalesce(
                    nodeFactory.createBranch(getPath(), getValueWrapper(), getLeftChild(), newChild), nodeFactory);
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
