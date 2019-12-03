package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Vanilla implementation of a {@link UniNodeFactory}.
 *
 * @author ppedemon
 */
public class DefaultUniNodeFactory implements UniNodeFactory {

    @Override
    public UniNode createLeaf(final BytesValue path, final BytesValue value) {
        return new BranchUniNode(path, value, this);
    }

    @Override
    public UniNode createBranch(
            final BytesValue path,
            final BytesValue value,
            final UniNode leftChild,
            final UniNode rightChild) {
        return new BranchUniNode(path, value, leftChild, rightChild, this);
    }
}
