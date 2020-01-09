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

/**
 * Trie storage mode: we can use a "classic" merkle patricia tree model
 * as described in the yellow paper, or a Unitrie.
 *
 * <p>For a detailed discussion of Unitrie-based storage, check:
 *    https://blog.rsk.co/noticia/towards-higher-onchain-scalability-with-the-unitrie/
 *
 * @author ppedemon
 */
public enum MerkleStorageMode {
  CLASSIC, UNITRIE;

  public static MerkleStorageMode fromString(final String str) {
    for (final MerkleStorageMode mode : MerkleStorageMode.values()) {
      if (mode.name().equalsIgnoreCase(str)) {
        return mode;
      }
    }
    return null;
  }

  public String toCLI() {
    return String.format("--merkle-storage-mode=%s", this.toString().toLowerCase());
  }
}
