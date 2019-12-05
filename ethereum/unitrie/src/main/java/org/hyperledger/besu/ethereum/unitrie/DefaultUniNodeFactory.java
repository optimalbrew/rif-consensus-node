package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Vanilla implementation of a {@link UniNodeFactory}.
 *
 * @author ppedemon
 */
public class DefaultUniNodeFactory implements UniNodeFactory {

    private final MerkleStorage storage;

    public DefaultUniNodeFactory(final MerkleStorage storage) {
        this.storage = storage;
    }

    @Override
    public UniNode createLeaf(final BytesValue path, final ValueWrapper valueWrapper) {
        return new BranchUniNode(path, valueWrapper, storage, this);
    }

    @Override
    public UniNode createBranch(
            final BytesValue path,
            final ValueWrapper valueWrapper,
            final UniNode leftChild,
            final UniNode rightChild) {
        return new BranchUniNode(path, valueWrapper, leftChild, rightChild, storage, this);
    }
}
