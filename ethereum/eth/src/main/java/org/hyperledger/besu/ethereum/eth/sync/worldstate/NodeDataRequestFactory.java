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
import org.hyperledger.besu.ethereum.merkleutils.ClassicMerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProviderVisitor;
import org.hyperledger.besu.ethereum.merkleutils.UniTrieMerkleAwareProvider;

/**
 * Create merkle storage-dependent {@link NodeDataRequest} instances.
 *
 * @author ppedemon
 */
class NodeDataRequestFactory implements MerkleAwareProviderVisitor<NodeDataRequest> {

  static NodeDataRequest createNodeDataRequest(
      final MerkleAwareProvider merkleAwareProvider, final Hash hash) {
    return merkleAwareProvider.accept(new NodeDataRequestFactory(hash));
  }

  private final Hash hash;

  private NodeDataRequestFactory(final Hash hash) {
    this.hash = hash;
  }

  @Override
  public NodeDataRequest visit(final ClassicMerkleAwareProvider provider) {
    return NodeDataRequest.createAccountDataRequest(hash);
  }

  @Override
  public NodeDataRequest visit(final UniTrieMerkleAwareProvider provider) {
    return NodeDataRequest.createUniNodeDataRequest(hash);
  }
}
