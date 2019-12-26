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
package org.hyperledger.besu.ethereum.triestorage;

import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Properties for classic trie storage (classic, as defined in the Yellow
 * paper Ethereum specification).
 *
 * @author ppedemon
 */
public class ClassicTrieStorage implements TrieStorage {

  @Override
  public BytesValue emptyTrieNodeEncoding() {
    return MerklePatriciaTrie.EMPTY_TRIE_NODE;
  }

  @Override
  public Bytes32 emptyTrieNodeHash() {
    return MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH;
  }
}
