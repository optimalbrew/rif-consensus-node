package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Interface defining contract for {@link UniNode} factory classes.
 *
 * @author ppedemon
 */
public interface UniNodeFactory {

    /**
     * Create a {@link UniNode} without children (that is, a leaf node).
     *
     * @param path   path of node to create
     * @param value  value of node to create
     * @return  new leaf node
     */
    UniNode createLeaf(BytesValue path, BytesValue value);

    /**
     * Create a branch {@link UniNode}.
     *
     * @param path        path of node to create
     * @param value       value of node to create
     * @param leftChild   left hand side child of node to create
     * @param rightChild  right hand side child of node to create
     * @return  new branch node
     */
    UniNode createBranch(BytesValue path, BytesValue value, UniNode leftChild, UniNode rightChild);
}
