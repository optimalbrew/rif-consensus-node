package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * Class encompassing {@link UniNode} data to be encoded.
 *
 * @author ppedemon
 */
class UniNodeEncodingInput {

  private byte[] path;
  private ValueWrapper valueWrapper;
  private final UniNode leftChild;
  private final UniNode rightChild;

  UniNodeEncodingInput(final byte[] path, final ValueWrapper valueWrapper) {
    this(path, valueWrapper, NullUniNode.instance(), NullUniNode.instance());
  }

  UniNodeEncodingInput(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild) {

    Preconditions.checkNotNull(path);
    Preconditions.checkNotNull(valueWrapper);
    Preconditions.checkNotNull(leftChild);
    Preconditions.checkNotNull(rightChild);

    this.path = path;
    this.valueWrapper = valueWrapper;
    this.leftChild = leftChild;
    this.rightChild = rightChild;
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
    if (isLeaf()) {
      return 0;
    } else {
      return leftChild.intrinsicSize() + rightChild.intrinsicSize();
    }
  }

  boolean isLeaf() {
    return leftChild == NullUniNode.instance() && rightChild == NullUniNode.instance();
  }
}
