package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * A leaf UniNode.
 *
 * @author ppedemon
 */
public class LeafUniNode extends EncodedUniNode {

  LeafUniNode(final byte[] path, final ValueWrapper valueWrapper) {
    super(path, valueWrapper);
    encoding = encode(path, valueWrapper);
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
    int valueSize = valueWrapper.isLong() ? valueWrapper.getLength().orElse(0) : 0;
    return (long) valueSize + getEncoding().length;
  }


  @Override
  public boolean isReferencedByHash() {
    return getEncoding().length > MAX_INLINED_NODE_SIZE;
  }

  private byte[] encode(final byte[] path, final ValueWrapper valueWrapper) {
    UniNodeEncodingData encData = new UniNodeEncodingData(path, valueWrapper);
    return encodingHelper.encode(encData).getArrayUnsafe();
  }
}
