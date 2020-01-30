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
 *
 */
package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Lexicographically collect values from some {@link UniNode}, starting from a given path.
 *
 * @param <V> type of values returned by this collector
 * @author ppedemon
 */
public class UniTrieCollector<V> implements UniTrieIterator.LeafHandler {

  private final BytesValue startPath;
  private final int limit;
  private final Map<BytesValue, V> values = new TreeMap<>();
  private final Function<BytesValue, V> valueDeserializer;

  public UniTrieCollector(
      final BytesValue startPath,
      final int limit,
      final Function<BytesValue, V> valueDeserializer) {

    this.startPath = startPath;
    this.limit = limit;
    this.valueDeserializer = valueDeserializer;
  }

  public static <V> Map<BytesValue, V> collectEntries(
      final UniNode root,
      final BytesValue startPath,
      final int limit,
      final Function<BytesValue, V> valueDeserializer) {

    final UniTrieCollector<V> collector =
        new UniTrieCollector<>(startPath, limit, valueDeserializer);
    final UniTrieIterator visitor = new UniTrieIterator(collector);
    root.accept(visitor, PathEncoding.decodePath(startPath, startPath.size() * 8));
    return collector.getValues();
  }

  private boolean limitReached() {
    return limit <= values.size();
  }

  @Override
  public UniTrieIterator.State onLeaf(final BytesValue path, final UniNode node) {
    if (path.compareTo(startPath) >= 0) {
      node.getValue().ifPresent(value -> values.put(path, valueDeserializer.apply(value)));
    }
    return limitReached() ? UniTrieIterator.State.STOP : UniTrieIterator.State.CONTINUE;
  }

  public Map<BytesValue, V> getValues() {
    return values;
  }
}
