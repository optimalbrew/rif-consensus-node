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
      "f37871146b918e6e79df191c9fe21d40da642acd094abe9f7705d8f469d331c0";

  // Ditto OLYMPIC_HASH
  private static final String OLYMPIC_RLP =
      "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a01638cbb4db2216d9e0ec3fc0a40e228c4083f11274becd52998ab1c87367749ea056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000080832fefd8808080a0000000000000000000000000000000000000000000000000000000000000000088000000000000002ac0c0";

  private static final MerkleAwareProvider merkleAwareProvider = new UniTrieMerkleAwareProvider();

  @Test
  public void createFromJsonWithAllocs() throws Exception {
    doCreateFromJsonWithAllocs(
        merkleAwareProvider,
        Hash.fromHexString("0xfce41de0911e185513749cb6a39daa7e56201f8a3dfcd13763e1ca8967a87f7e"));
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
        "0xbcaeb44239dd06cc81964b241d7708745b65247cb6b3117a1fc39073390e6770",
        0);
  }

  @Test
  public void createFromJsonWithVersion() throws Exception {
    assertContractInvariants(
        merkleAwareProvider,
        "genesis4.json",
        "0x0b8d391e8bd4eb4b9ed6858273309367de88fbc45f32db9e48b978501798586b",
        1);
  }

  @Test
  public void createFromJsonWithNonce() throws Exception {
    doCreateFromJsonWithNonce(
        merkleAwareProvider,
        Hash.fromHexString("0x6f0d529af164d0daf0cfc4ac62467958311ec48f9f2acd649919373441cd4b98"));
  }

  @Test
  public void encodeOlympicBlock() throws Exception {
    doEncodeOlympicBlock(merkleAwareProvider, OLYMPIC_HASH, OLYMPIC_RLP);
  }
}
