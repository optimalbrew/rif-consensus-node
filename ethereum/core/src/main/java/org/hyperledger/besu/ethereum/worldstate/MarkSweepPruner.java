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
package org.hyperledger.besu.ethereum.worldstate;

import org.hyperledger.besu.ethereum.core.Hash;

/**
 * Interface for mark-sweep storage collectors.
 *
 * @author ppedemon
 */
public interface MarkSweepPruner {

  /** Prepare for a mark-sweep round */
  void prepare();

  /**
   * Mark nodes in the state specified by the given hash.
   *
   * @param rootHash root hash of state whose nodes will be marked
   */
  void mark(Hash rootHash);

  /**
   * Sweep unmarked nodes for all blocks older than the block with the given number.
   *
   * @param markedBlockNumber number of last marked block, unmarked nodes for older blocks must go
   */
  void sweepBefore(long markedBlockNumber);

  /** Cleanup this storage collector */
  void cleanup();
}
