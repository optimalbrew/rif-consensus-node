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
import org.hyperledger.besu.ethereum.worldstate.UniTrieMutableWorldState;
import org.hyperledger.besu.ethereum.worldstate.WorldStatePreimageStorage;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.util.bytes.Bytes32;

/**
 * Provider tailored to Untrie Merkle storage.
 *
 * @author ppedemon
 */
public class UniTrieMerkleAwareProvider implements MerkleAwareProvider {

  @Override
  public MutableWorldState createMutableWorldState(
      final WorldStateStorage storage, final WorldStatePreimageStorage preImageStorage) {
    return new UniTrieMutableWorldState(storage);
  }

  @Override
  public MutableWorldState createMutableWorldState(
      final Bytes32 rootHash,
      final WorldStateStorage storage,
      final WorldStatePreimageStorage preImageStorage) {
    return new UniTrieMutableWorldState(rootHash, storage);
  }
}
