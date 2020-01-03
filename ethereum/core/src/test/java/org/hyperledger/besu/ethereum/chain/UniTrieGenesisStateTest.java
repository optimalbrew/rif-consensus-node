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
package org.hyperledger.besu.ethereum.chain;

import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.UniTrieMerkleAwareProvider;
import org.junit.Test;

public class UniTrieGenesisStateTest extends AbstractGenesisStateTest {

  // State root hash differs when using Unitries, so this will be different from the known one
  private static final String OLYMPIC_HASH =
      "5e36bc99fb87f50b3c3ea37e8ed6893679c37bc62e108b657f810161da14e5c5";

  // Ditto OLYMPIC_HASH
  private static final String OLYMPIC_RLP =
      "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a0dcdf011ebf8a31480e0b3df1297b8d01fbc6996f1d1b41b62c8ae16a13aa96dca056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000080832fefd8808080a0000000000000000000000000000000000000000000000000000000000000000088000000000000002ac0c0";

  private static final MerkleAwareProvider merkleAwareProvider = new UniTrieMerkleAwareProvider();

  @Test
  public void createFromJsonWithAllocs() throws Exception {
    doCreateFromJsonWithAllocs(
        merkleAwareProvider,
        Hash.fromHexString("0xe7dcf0920784a73f2b64c358118e0df6ee57a7dfb724a575bf703673434e2fbb"));
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
        "0x16f7d2b97a73c0e5fad5cf25ac73698ca4216e19695b84e0519a6da758835844",
        0);
  }

  @Test
  public void createFromJsonWithVersion() throws Exception {
    assertContractInvariants(
        merkleAwareProvider,
        "genesis4.json",
        "0xa10351c41eda28b857c4adf4e2a572505f30248a1eb6cdfdfc4f31b2e42aa328",
        1);
  }

  @Test
  public void createFromJsonWithNonce() throws Exception {
    doCreateFromJsonWithNonce(
        merkleAwareProvider,
        Hash.fromHexString("0x171aab28694538350070405eba1982805b2f4d7cfd87e62667bfc5ef4deddf81"));
  }

  @Test
  public void encodeOlympicBlock() throws Exception {
    doEncodeOlympicBlock(merkleAwareProvider, OLYMPIC_HASH, OLYMPIC_RLP);
  }
}
