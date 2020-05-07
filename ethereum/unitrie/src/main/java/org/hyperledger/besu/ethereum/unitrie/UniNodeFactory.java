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

/**
 * Interface defining contract for {@link UniNode} factory classes.
 *
 * @author ppedemon
 */
public interface UniNodeFactory {

  /**
   * Create a {@link UniNode} without children (that is, a leaf node).
   *
   * @param path path of node to create
   * @param valueWrapper value of node to create
   * @return new leaf node
   */
  UniNode createLeaf(byte[] path, ValueWrapper valueWrapper);

  /**
   * Create a branch {@link UniNode} with unknown children size.
   *
   * @param path path of node to create
   * @param valueWrapper value of node to create
   * @param leftChild left hand side child of node to create
   * @param rightChild right hand side child of node to create
   * @return new branch node
   */
  UniNode createBranch(
      byte[] path, ValueWrapper valueWrapper, UniNode leftChild, UniNode rightChild);
}
