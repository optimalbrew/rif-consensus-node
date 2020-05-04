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
package org.hyperledger.besu.ethereum.unitrie;

import org.apache.tuweni.bytes.Bytes;

/**
 * Interface for path-aware Unitrie visitors.
 *
 * @author ppedemon
 */
public interface UniPathVisitor {

  /**
   * Visit a {@link NullUniNode}.
   *
   * @param node node to visit
   * @param path path leading to the visited node
   * @return node resulting from visit
   */
  UniNode visit(NullUniNode node, Bytes path);

  /**
   * Visit an {@link AbstractUniNode}.
   *
   * @param node node to visit
   * @param path path leading to visited node
   * @return node resulting from visit
   */
  UniNode visit(AbstractUniNode node, Bytes path);
}
