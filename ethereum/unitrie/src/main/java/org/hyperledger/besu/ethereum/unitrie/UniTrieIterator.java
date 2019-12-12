/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Path visitor traversing a Unitrie.
 *
 * Given a valid path it iterates the Unitrie from it, invoking a callback
 * for each leaf. Such callback receives the full path to a leaf plus the
 * leaf itself. It's up to the callback to determine whether the iteration
 * must continue or stop.
 *
 * In general, this iterator is used to collect a range of leaves for a
 * given Unitrie.
 *
 * @author ppedemon
 */
public class UniTrieIterator implements UniPathVisitor {

    /**
     * Iteration state:
     *
     *   SEARCHING: look for initial leaf to collect.
     *   CONTINUE: first leaf to collect found, from now collect every leaf we find.
     *   STOP: stop iteration, ignore further leaves.
     */
    public enum State {
        SEARCHING, CONTINUE, STOP;

        public boolean continueIterating() {
            return this != STOP;
        }
    }

    /**
     * Leaf handler: given a leaf node and its path interpreted as a 32 bytes hash,
     * process the leaf and return the new iteration state.
     */
    @FunctionalInterface
    public interface LeafHandler {
        State onLeaf(Bytes32 keyHash, UniNode node);
    }

    private final Deque<BytesValue> paths = new ArrayDeque<>();
    private final LeafHandler leafHandler;
    private State state = State.SEARCHING;

    public UniTrieIterator(final LeafHandler leafHandler) {
        this.leafHandler = leafHandler;
    }

    @Override
    public UniNode visit(final NullUniNode node, final BytesValue path) {
        state = State.CONTINUE;
        return node;
    }

    @Override
    public UniNode visit(final BranchUniNode node, final BytesValue path) {
        if (node.isLeaf()) {
            return handleLeaf(node);
        }

        if (path.isEmpty()) {
            return node;
        }

        byte pos = 0;
        BytesValue remainingPath = path;
        if (state == State.SEARCHING) {
            pos = path.get(0);
            remainingPath = path.slice(1);
        }
        paths.push(node.getPath());

        if (pos == 0 && state.continueIterating()) {
            handleChild(node.getLeftChild(), pos, remainingPath);

            // Set pos = 1, so when starting from the left child we will fallback into visiting
            // the right child, provided the visit to the left didn't set the state to STOP.
            pos = 1;
        }
        if (pos == 1 && state.continueIterating()) {
            handleChild(node.getRightChild(), pos, remainingPath);
        }

        paths.pop();
        return node;
    }

    private void handleChild(final UniNode child, final byte pos, final BytesValue remainingPath) {
        paths.push(BytesValue.of(pos));
        child.accept(this, remainingPath);
        paths.pop();
    }

    private UniNode handleLeaf(final BranchUniNode node) {
        paths.push(node.getPath());
        state = leafHandler.onLeaf(keyHash(), node);
        paths.pop();
        return node;
    }

    private Bytes32 keyHash() {
        final Iterator<BytesValue> iterator = paths.descendingIterator();
        BytesValue fullPath = iterator.next();
        while (iterator.hasNext()) {
            fullPath = BytesValue.wrap(fullPath, iterator.next());
        }
        return fullPath.isZero()
                ? Bytes32.ZERO
                : Bytes32.wrap(PathEncoding.encodePath(fullPath), 0);
    }
}
