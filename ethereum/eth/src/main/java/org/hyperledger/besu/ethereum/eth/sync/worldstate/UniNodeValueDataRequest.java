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
package org.hyperledger.besu.ethereum.eth.sync.worldstate;

import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage.Updater;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;

/**
 * Request UniNode long value.
 *
 * @author ppedemon
 */
public class UniNodeValueDataRequest extends NodeDataRequest {

  public UniNodeValueDataRequest(final Hash hash) {
    super(RequestType.UNINODE_VALUE, hash);
  }

  @Override
  protected void doPersist(final Updater updater) {
    updater.rawPut(getHash(), getData());
  }

  @Override
  public Stream<NodeDataRequest> getChildRequests() {
    return Stream.empty();
  }

  @Override
  public Optional<Bytes> getExistingData(final WorldStateStorage worldStateStorage) {
    // getAccountStateTrieNode works for retrieving arbitrary data from storage,
    // so it's ok to use it here
    return worldStateStorage.getAccountStateTrieNode(getHash());
  }
}
