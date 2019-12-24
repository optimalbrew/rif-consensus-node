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

import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SimpleUniTrieTest extends AbstractUniTrieTest {

  @Override
  MerklePatriciaTrie<BytesValue, String> createTrie() {
    return new SimpleUniTrie<>(
        s -> Objects.isNull(s) ? null : BytesValue.wrap(s.getBytes(StandardCharsets.UTF_8)),
        v -> new String(v.getArrayUnsafe(), StandardCharsets.UTF_8));
  }
}
