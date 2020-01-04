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
package org.hyperledger.besu.ethereum.merkleutils;

import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.proof.WorldStateProofProvider;
import org.hyperledger.besu.ethereum.worldstate.WorldStatePreimageStorage;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.util.bytes.Bytes32;

/**
 * Abstraction layer for creating instances of classes depending on
 * different Merkle storage models.
 *
 * @author ppedemon
 */
public interface MerkleAwareProvider {

  /**
   * Create a new {@link MutableWorldState} instance.
   *
   * @param storage world state storage
   * @param preImageStorage world state pre-image storage
   * @return {@link MutableWorldState} instance
   */
  MutableWorldState createMutableWorldState(
      WorldStateStorage storage, WorldStatePreimageStorage preImageStorage);

  /**
   * Create a new {@link MutableWorldState} instance.
   *
   * @param rootHash root hash of the state represented by the instance to create
   * @param storage world state storage
   * @param preImageStorage world state pre-image storage
   * @return {@link MutableWorldState} instance
   */
  MutableWorldState createMutableWorldState(
      Bytes32 rootHash, WorldStateStorage storage, WorldStatePreimageStorage preImageStorage);

  /**
   * Create a new {@link WorldStateProofProvider}.
   *
   * @param storage storage for returned provider
   * @return {@link WorldStateProofProvider} instance
   */
  WorldStateProofProvider createWorldStateProofProvider(WorldStateStorage storage);
}
