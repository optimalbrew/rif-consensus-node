package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * Class encompassing {@link UniNode} data to be encoded.
 *
 * @author ppedemon
 */
class UniNodeEncodingData {

    private byte[] path;
    private ValueWrapper valueWrapper;
    private final UniNode leftChild;
    private final UniNode rightChild;

    private long childrenSize;

    UniNodeEncodingData(final byte[] path, final ValueWrapper valueWrapper) {
        this(path, valueWrapper, NullUniNode.instance(), NullUniNode.instance(), 0);
    }

    UniNodeEncodingData(
            final byte[] path,
            final ValueWrapper valueWrapper,
            final UniNode leftChild,
            final UniNode rightChild,
            final long childrenSize) {

        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(valueWrapper);
        Preconditions.checkNotNull(leftChild);
        Preconditions.checkNotNull(rightChild);

        this.path = path;
        this.valueWrapper = valueWrapper;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.childrenSize = childrenSize;
    }

    public byte[] getPath() {
        return path;
    }

    public ValueWrapper getValueWrapper() {
        return valueWrapper;
    }

    public Optional<Integer> getValueLength() {
        return valueWrapper.getLength();
    }

    public UniNode getLeftChild() {
        return leftChild;
    }

    public UniNode getRightChild() {
        return rightChild;
    }

    long getChildrenSize() {
        if (childrenSize == -1) {
            if (isLeaf()) {
                childrenSize = 0;
            } else {
                childrenSize = leftChild.intrinsicSize() + rightChild.intrinsicSize();
            }
        }
        return childrenSize;
    }

    boolean isLeaf() {
        return leftChild == NullUniNode.instance() && rightChild == NullUniNode.instance();
    }
}

