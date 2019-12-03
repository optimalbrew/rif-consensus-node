package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;

/**
 * Interface for nodes in a Unitrie. A node has a path and an optional value.
 *
 * @author ppedemon
 */
public interface UniNode {

    /**
     * Get path for this node.
     *
     * @return path for this node
     */
    BytesValue getPath();

    /**
     * Get optional value for this node.
     *
     * @return  optional node value
     */
    Optional<BytesValue> getValue();

    /**
     * Get left hand side child of this node.
     *
     * @return  left hand side child of this node
     */
    UniNode getLeftChild();

    /**
     * Get right hand side child of this node.
     *
     * @return  right hand side child of this node
     */
    UniNode getRightChild();

    /**
     * Pretty print a Uninode.
     *
     * @param indent  indentation to prepend
     * @return  string representation of a Uninode
     */
    String print(final int indent);

    /**
     * Accept a {@link UniPathVisitor}.
     *
     * @param visitor  visitor instance
     * @param path  path leading to this node
     * @return  node resulting form visit
     */
    UniNode accept(UniPathVisitor visitor, BytesValue path);
}
