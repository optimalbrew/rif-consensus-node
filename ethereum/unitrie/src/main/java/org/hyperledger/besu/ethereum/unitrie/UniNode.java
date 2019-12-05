package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.util.bytes.Bytes32;
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
     * Get the wrapper for this value.
     *
     * @return   value wrapper for this node
     */
    ValueWrapper getValueWrapper();

    /**
     * Get optional value for this node.
     *
     * @return  optional holding node's value
     */
    Optional<BytesValue> getValue();

    /**
     * If value is present return its hash.
     *
     * @return  optional holding value's hash if present
     */
    Optional<Bytes32> getValueHash();

    /**
     * If value is present return its length in bytes.
     *
     * @return  optional holding value's length in bytes if present
     */
    Optional<UInt24> getValueLength();

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
