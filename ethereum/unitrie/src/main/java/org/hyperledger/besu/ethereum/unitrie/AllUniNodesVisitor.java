/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 */
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.ethereum.trie.BasicNode;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Traverse a whole Unitrie, node by node.
 *
 * @param <V>  type of values handled by this visitor
 * @author ppedemon
 */
public class AllUniNodesVisitor<V> implements UniNodeVisitor {

    /**
     * Basic node implementation, so it's possible to visit Unitrie
     * nodes without leaking the underlying representation.
     *
     * @param <V>  type of value held by the node
     */
    private static class BasicNodeImpl<V> implements BasicNode<V> {
        private final V value;
        private final Bytes32 hash;

        BasicNodeImpl(final V value, final Bytes32 hash) {
            this.value = value;
            this.hash = hash;
        }

        @Override
        public Bytes32 getHash() {
            return hash;
        }

        @Override
        public Optional<V> getValue() {
            return Optional.ofNullable(value);
        }
    }

    private final Function<BytesValue, V> valueDeserializer;
    private final Consumer<BasicNode<V>> handler;

    public AllUniNodesVisitor(final Function<BytesValue, V> valueDeserializer, final Consumer<BasicNode<V>> handler) {
        this.valueDeserializer = valueDeserializer;
        this.handler = handler;
    }

    @Override
    public void visit(final NullUniNode node) {
    }

    @Override
    public void visit(final BranchUniNode node) {
        BasicNode<V> n = new BasicNodeImpl<>(
                node.getValue().map(valueDeserializer).orElseGet(() -> null),
                node.getHash());
        handler.accept(n);
        acceptAndUnload(node.getLeftChild());
        acceptAndUnload(node.getRightChild());
    }

    private void acceptAndUnload(final UniNode node) {
        node.accept(this);
        node.unload();
    }
}
