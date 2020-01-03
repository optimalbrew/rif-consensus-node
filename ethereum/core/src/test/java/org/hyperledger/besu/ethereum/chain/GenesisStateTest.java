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
package org.hyperledger.besu.ethereum.chain;

import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.merkleutils.ClassicMerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.junit.Test;

public final class GenesisStateTest extends AbstractGenesisStateTest {

  /** Known RLP encoded bytes of the Olympic Genesis Block. */
  private static final String OLYMPIC_RLP =
      "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a09178d0f23c965d81f0834a4c72c6253ce6830f4022b1359aaebfc1ecba442d4ea056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000080832fefd8808080a0000000000000000000000000000000000000000000000000000000000000000088000000000000002ac0c0";

  /** Known Hash of the Olympic Genesis Block. */
  private static final String OLYMPIC_HASH =
      "fd4af92a79c7fc2fd8bf0d342f2e832e1d4f485c85b9152d2039e03bc604fdca";

  private static final MerkleAwareProvider merkleAwareProvider = new ClassicMerkleAwareProvider();

  @Test
  public void createFromJsonWithAllocs() throws Exception {
    doCreateFromJsonWithAllocs(
        merkleAwareProvider,
        Hash.fromHexString("0x92683e6af0f8a932e5fe08c870f2ae9d287e39d4518ec544b0be451f1035fd39"));
  }

  @Test
  public void createFromJsonNoAllocs() throws Exception {
    doCreateFromJsonNoAllocs(merkleAwareProvider);
  }

  @Test
  public void createFromJsonWithContract() throws Exception {
    assertContractInvariants(
        merkleAwareProvider,
        "genesis3.json",
        "0xe7fd8db206dcaf066b7c97b8a42a0abc18653613560748557ab44868652a78b6",
        0);
  }

  @Test
  public void createFromJsonWithVersion() throws Exception {
    assertContractInvariants(
        merkleAwareProvider,
        "genesis4.json",
        "0x3224ddae856381f5fb67492b4561ecbc0cb1e9e50e6cf3238f6e049fe95a8604",
        1);
  }

  @Test
  public void createFromJsonWithNonce() throws Exception {
    doCreateFromJsonWithNonce(
        merkleAwareProvider,
        Hash.fromHexString("0x36750291f1a8429aeb553a790dc2d149d04dbba0ca4cfc7fd5eb12d478117c9f"));
  }

  @Test
  public void encodeOlympicBlock() throws Exception {
    doEncodeOlympicBlock(merkleAwareProvider, OLYMPIC_HASH, OLYMPIC_RLP);
  }
}
