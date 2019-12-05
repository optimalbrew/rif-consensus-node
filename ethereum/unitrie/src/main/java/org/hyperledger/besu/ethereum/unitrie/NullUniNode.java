package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Strings;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;

/**
 * Empty Unitrie node.
 *
 * @author ppedemon
 */
public final class NullUniNode implements UniNode {

    private static NullUniNode INSTANCE = new NullUniNode();

    public static NullUniNode instance() {
        return INSTANCE;
    }

    private NullUniNode() {
        super();
    }

    @Override
    public BytesValue getPath() {
        return BytesValue.EMPTY;
    }

    @Override
    public ValueWrapper getValueWrapper() {
        return null;
    }

    @Override
    public Optional<BytesValue> getValue() {
        return Optional.empty();
    }

    @Override
    public Optional<Bytes32> getValueHash() {
        return Optional.empty();
    }

    @Override
    public Optional<UInt24> getValueLength() {
        return Optional.empty();
    }

    @Override
    public UniNode getLeftChild() {
        return null;
    }

    @Override
    public UniNode getRightChild() {
        return null;
    }

    @Override
    public String print(final int indent) {
        return String.format("%s[null]", Strings.repeat(" ", indent));
    }

    @Override
    public String toString() {
        return print(0);
    }

    @Override
    public UniNode accept(final UniPathVisitor visitor, final BytesValue path) {
        return visitor.visit(this, path);
    }
}
