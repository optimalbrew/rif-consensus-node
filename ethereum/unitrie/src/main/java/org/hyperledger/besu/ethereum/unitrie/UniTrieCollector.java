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

import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Lexicographically collect values from some {@link UniNode}, starting from a given hash.
 *
 * @param <V>  type of values returned by this collector
 * @author ppedemon
 */
public class UniTrieCollector<V> implements UniTrieIterator.LeafHandler {

    private final Bytes32 startKeyHash;
    private final int limit;
    private final Map<Bytes32, V> values = new TreeMap<>();
    private final Function<BytesValue, V> valueDeserializer;

    public UniTrieCollector(
            final Bytes32 startKeyHash,
            final int limit,
            final Function<BytesValue, V> valueDeserializer) {

        this.startKeyHash = startKeyHash;
        this.limit = limit;
        this.valueDeserializer = valueDeserializer;
    }

    public static <V> Map<Bytes32, V> collectEntries(
            final UniNode root,
            final Bytes32 startKeyHash,
            final int limit,
            final Function<BytesValue, V> valueDeserializer) {

        final UniTrieCollector<V> collector = new UniTrieCollector<>(startKeyHash, limit, valueDeserializer);
        final UniTrieIterator visitor = new UniTrieIterator(collector);
        root.accept(visitor, PathEncoding.decodePath(startKeyHash, Bytes32.SIZE * 8));
        return collector.getValues();
    }

    private boolean limitReached() {
        return limit <= values.size();
    }

    @Override
    public UniTrieIterator.State onLeaf(final Bytes32 keyHash, final UniNode node) {
        if (keyHash.compareTo(startKeyHash) >= 0) {
            node.getValue().ifPresent(value -> values.put(keyHash, valueDeserializer.apply(value)));
        }
        return limitReached() ? UniTrieIterator.State.STOP : UniTrieIterator.State.CONTINUE;
    }

    public Map<Bytes32, V> getValues() {
        return values;
    }
}
