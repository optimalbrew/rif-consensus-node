package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import org.apache.tuweni.bytes.Bytes;
/**
 * A leaf UniNode.
 *
 * @author ppedemon
 */
public class LeafUniNode extends AbstractUniNode {

  private final byte[] encoding;

  public LeafUniNode(final byte[] path, final ValueWrapper valueWrapper) {
    super(path, valueWrapper);
    encoding = encode(path, valueWrapper);
  }

  LeafUniNode(final UniNodeEncodingOutput encodingOutput) {
    super(encodingOutput.getPath(), encodingOutput.getValueWrapper());

    Preconditions.checkNotNull(encodingOutput.getEncoding());
    encoding = encodingOutput.getEncoding();
  }

  @Override
  public UniNode accept(final UniPathVisitor visitor, final Bytes path) {
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
  public byte[] getEncoding() {
    return encoding;
  }

  @Override
  public boolean isReferencedByHash() {
    return getEncoding().length > MAX_INLINED_NODE_SIZE;
  }

  private byte[] encode(final byte[] path, final ValueWrapper valueWrapper) {
    UniNodeEncodingInput encData = new UniNodeEncodingInput(path, valueWrapper);
    return encodingHelper.encode(encData).getArrayUnsafe();
  }
}
