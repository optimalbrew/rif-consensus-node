package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.Bytes32;

/**
 * Holder for value optionally held in a unitrie node.
 *
 * @param <V> Value's type
 * @author ppedemon
 */
public final class NodeValue<V> {

    // Representation invariant:
    //  - Empty value: value null, hash null, length = 0.
    //  - Value in storage: value null, hash set (storage key), length set.
    //  - Value loaded: value set, valueHash set, length set.

    /*
    private V value;
    private UInt24 length;
    private Bytes32 hash;
    */
}
