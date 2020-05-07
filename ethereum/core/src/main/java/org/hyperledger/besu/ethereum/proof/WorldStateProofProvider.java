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
 */
package org.hyperledger.besu.ethereum.proof;

import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Hash;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt256;

/**
 * Specification for classes that can provide world state Merkle proofs.
 *
 * @author ppedemon
 */
public interface WorldStateProofProvider {

  /**
   * Provide a world state proof for the given account and storage keys.
   *
   * @param worldStateRoot world state root hash
   * @param accountAddress account address
   * @param accountStorageKeys storage keys for the account to process
   * @return optional world state proof, empty if: there's world state for the given root hash or
   *     account doesn't exist for the given state
   */
  Optional<WorldStateProof> getAccountProof(
      Hash worldStateRoot, Address accountAddress, List<UInt256> accountStorageKeys);
}
