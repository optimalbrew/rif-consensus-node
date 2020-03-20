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
import org.hyperledger.besu.ethereum.unitrie.UniNode;
import org.hyperledger.besu.ethereum.unitrie.UniTrieNodeDecoder;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage.Updater;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Base class for UniNode data requests.
 *
 * @author ppedemon
 */
public class UniNodeDataRequest extends NodeDataRequest {

  UniNodeDataRequest(final Hash hash) {
    super(RequestType.UNINODE, hash);
  }

  @Override
  public Stream<NodeDataRequest> getChildRequests() {
    if (getData() == null) {
      // If this node hasn't been downloaded yet, we can't return any child data
      return Stream.empty();
    }

    UniTrieNodeDecoder decoder = new UniTrieNodeDecoder(__ -> Optional.empty());

    final List<UniNode> nodes = decoder.decodeNodes(getData());
    return nodes.stream()
        .flatMap(
            node -> {
              if (nodeIsHashReferencedDescendant(node)) {
                return Stream.of(new UniNodeDataRequest(bytesToHash(node.getHash())));
              } else if (node.getValueWrapper().isLong()) {
                return node.getValueHash().stream()
                    .map(h -> new UniNodeValueDataRequest(bytesToHash(h)));
              } else {
                return Stream.empty();
              }
            });
  }

  @Override
  protected void doPersist(final Updater updater) {
    updater.putAccountStateTrieNode(getHash(), getData());
  }

  @Override
  public Optional<BytesValue> getExistingData(final WorldStateStorage worldStateStorage) {
    return worldStateStorage.getAccountStateTrieNode(getHash());
  }

  private boolean nodeIsHashReferencedDescendant(final UniNode node) {
    return !Objects.equals(bytesToHash(node.getHash()), getHash()) && node.isReferencedByHash();
  }

  private Hash bytesToHash(final byte[] bytes) {
    return Hash.wrap(Bytes32.wrap(bytes));
  }
}
