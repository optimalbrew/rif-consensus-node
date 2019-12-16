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
 */
package org.hyperledger.besu.ethereum.trie;

import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;

/**
 * A basic trie node, having a has and optionally a value.
 *
 * @param <V>  type of value held by this node
 * @author ppedemon
 */
public interface BasicNode<V> {

    /**
     * Get the hash corresponding to this trie node.
     *
     * @return  hash for this tree node
     */
    Bytes32 getHash();

    /**
     * Get optional value for this trie node.
     *
     * @return  optional holding value, or empty if value is absent
     */
    Optional<V> getValue();

    /**
     * Answer whether this node is referenced by a hash.
     *
     * @return whether this node is referenced by a hash
     */
    boolean isReferencedByHash();

    /**
     * Get node encoding.
     *
     * @return  node encoding
     */
    BytesValue getEncoding();
}
