package org.hyperledger.besu.ethereum.unitrie;

import com.google.common.base.Preconditions;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Visitor implementing Remove operation in a unitrie.
 *
 * @author ppedemon
 */
public class RemoveVisitor implements UniPathVisitor {

    @Override
    public UniNode visit(final NullUniNode node, final BytesValue path) {
        return NullUniNode.instance();
    }

    @Override
    public UniNode visit(final BranchUniNode node, final BytesValue path) {
        BytesValue nodePath = node.getPath();
        BytesValue commonPath = path.commonPrefix(nodePath);

        if (commonPath.size() == path.size() && commonPath.size() == nodePath.size()) {
            return node.removeValue();
        }

        if (commonPath.size() < nodePath.size()) {
            return node;
        }

        byte pos = path.get(commonPath.size());
        BytesValue newPath = path.slice(commonPath.size() + 1);
        if (pos == 0) {
            return node.replaceChild(pos, node.getLeftChild().accept(this, newPath));
        } else {
            return node.replaceChild(pos, node.getRightChild().accept(this, newPath));
        }
    }
}
