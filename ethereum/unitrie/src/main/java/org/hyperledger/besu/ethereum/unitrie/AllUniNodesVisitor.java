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
     * Basic node proxying a UniNode and making it polymorphic.
     * @param <V>  type of value held by the node
     */
    private static class UniNodeProxy<V> implements BasicNode<V> {
        private final UniNode node;
        private final Function<BytesValue, V> valueDeserializer;

        UniNodeProxy(final UniNode node, final Function<BytesValue, V> valueDeserializer) {
            this.valueDeserializer = valueDeserializer;
            this.node = node;
        }

        @Override
        public Bytes32 getHash() {
            return node.getHash();
        }

        @Override
        public Optional<V> getValue() {
            return node.getValue().map(valueDeserializer);
        }

        @Override
        public boolean isReferencedByHash() {
            return node.isReferencedByHash();
        }

        @Override
        public BytesValue getEncoding() {
            return node.getEncoding();
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
        UniNodeProxy<V> p = new UniNodeProxy<>(node, valueDeserializer);
        handler.accept(p);
        acceptAndUnload(node.getLeftChild());
        acceptAndUnload(node.getRightChild());
    }

    private void acceptAndUnload(final UniNode node) {
        node.accept(this);
        node.unload();
    }
}
