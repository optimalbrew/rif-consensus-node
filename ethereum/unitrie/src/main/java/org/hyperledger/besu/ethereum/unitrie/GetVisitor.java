package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Visitor implementing get operation in a Unitrie.
 *
 * @author ppedemon
 */
public class GetVisitor implements UniPathVisitor {

    @Override
    public UniNode visit(final NullUniNode node, final BytesValue path) {
        return NullUniNode.instance();
    }

    @Override
    public UniNode visit(final BranchUniNode node, final BytesValue path) {
        BytesValue nodePath = node.getPath();
        BytesValue commonPath = path.commonPrefix(nodePath);

        if (commonPath.size() == path.size() && commonPath.size() == nodePath.size()) {
            return node;
        }

        if (commonPath.size() < nodePath.size()) {
            return NullUniNode.instance();
        }

        byte pos = path.get(commonPath.size());
        BytesValue newPath = path.slice(commonPath.size() + 1);
        if (pos == 0) {
            return node.getLeftChild().accept(this, newPath);
        } else {
            return node.getRightChild().accept(this, newPath);
        }
    }
}
