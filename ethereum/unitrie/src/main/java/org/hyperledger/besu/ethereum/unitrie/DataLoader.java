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

import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;

/**
 * Functional interface modeling the capability to load data from key value storage.
 *
 * @author ppedemon
 */
@FunctionalInterface
public interface DataLoader {

  /**
   * Load some data from key value storage given its hash, used as key.
   *
   * @param hash hash of data to load
   * @return optional holding loaded data if found, else empty
   */
  Optional<BytesValue> load(Bytes32 hash);
}