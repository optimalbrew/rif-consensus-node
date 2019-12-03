package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Interface for path-aware Unitrie visitors.
 *
 * @author ppedemon
 */
public interface UniPathVisitor {

    /**
     * Visit a {@link NullUniNode}.
     *
     * @param node  node to visit
     * @param path  path leading to the visited node
     * @return  node resulting from visit
     */
    UniNode visit(NullUniNode node, BytesValue path);

    /**
     * Visit a {@link BranchUniNode}.
     *
     * @param node  node to visit
     * @param path  path leading to visited node
     * @return  node resulting from visit
     */
    UniNode visit(BranchUniNode node, BytesValue path);
}
