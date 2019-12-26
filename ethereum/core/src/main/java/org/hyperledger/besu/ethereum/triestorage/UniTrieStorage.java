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

import org.hyperledger.besu.ethereum.unitrie.UniNodeEncoding;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Properties for Unitrie-based trie storage.
 *
 * @author ppedemon
 */
public class UniTrieStorage implements TrieStorage {

  @Override
  public BytesValue emptyTrieNodeEncoding() {
    return UniNodeEncoding.NULL_UNINODE_ENCODING;
  }

  @Override
  public Bytes32 emptyTrieNodeHash() {
    return UniNodeEncoding.NULL_UNINODE_HASH;
  }
}
