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
package org.hyperledger.besu.ethereum.eth.sync.worldstate;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.core.BlockDataGenerator;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

public class NodeDataRequestTest {

  private static Hash VALID_UNINODE_HASH =
      Hash.fromHexString("0x05f6b870df41144da5eec8d1384189d11977cc543214268aa08eadeb34f6508d");

  private static BytesValue VALID_UNINODE_ENCODING =
      BytesValue.fromHexString(
          "0x787800b6979620706f8c652cfb1234567890123456789012345678901234567890d9f59f9bc38d49848" +
          "dd4b90bfd37aab80fa843602165db74f1afec0c87bb06ab8473f302fe9da10e548b03a0b099180d82f124" +
          "68b246e8131a55ab63960da4bb77000046");

  private static Hash VALID_UNINODE_VALUE_HASH =
      Hash.fromHexString("0x73f302fe9da10e548b03a0b099180d82f12468b246e8131a55ab63960da4bb77");

  private static Hash VALID_UNINODE_CHILD_HASH =
      Hash.fromHexString("0xd9f59f9bc38d49848dd4b90bfd37aab80fa843602165db74f1afec0c87bb06ab");

  @Test
  public void serializesAccountTrieNodeRequests() {
    BlockDataGenerator gen = new BlockDataGenerator(0);
    AccountTrieNodeDataRequest request = NodeDataRequest.createAccountDataRequest(gen.hash());
    NodeDataRequest sedeRequest = serializeThenDeserialize(request);
    assertRequestsEquals(sedeRequest, request);
    assertThat(sedeRequest).isInstanceOf(AccountTrieNodeDataRequest.class);
  }

  @Test
  public void serializesStorageTrieNodeRequests() {
    BlockDataGenerator gen = new BlockDataGenerator(0);
    StorageTrieNodeDataRequest request = NodeDataRequest.createStorageDataRequest(gen.hash());
    NodeDataRequest sedeRequest = serializeThenDeserialize(request);
    assertRequestsEquals(sedeRequest, request);
    assertThat(sedeRequest).isInstanceOf(StorageTrieNodeDataRequest.class);
  }

  @Test
  public void serializesCodeRequests() {
    BlockDataGenerator gen = new BlockDataGenerator(0);
    CodeNodeDataRequest request = NodeDataRequest.createCodeRequest(gen.hash());
    NodeDataRequest sedeRequest = serializeThenDeserialize(request);
    assertRequestsEquals(sedeRequest, request);
    assertThat(sedeRequest).isInstanceOf(CodeNodeDataRequest.class);
  }

  @Test
  public void serializesUniNodeRequests() {
    BlockDataGenerator gen = new BlockDataGenerator(0);
    UniNodeDataRequest request = NodeDataRequest.createUniNodeDataRequest(gen.hash());
    NodeDataRequest sedeRequest = serializeThenDeserialize(request);
    assertRequestsEquals(sedeRequest, request);
    assertThat(sedeRequest).isInstanceOf(UniNodeDataRequest.class);
  }

  @Test
  public void serializesUniNodeValueRequests() {
    BlockDataGenerator gen = new BlockDataGenerator(0);
    UniNodeValueDataRequest request = NodeDataRequest.createUniNodeValueDataRequest(gen.hash());
    NodeDataRequest sedeRequest = serializeThenDeserialize(request);
    assertRequestsEquals(sedeRequest, request);
    assertThat(sedeRequest).isInstanceOf(UniNodeValueDataRequest.class);
  }

  @Test
  public void uniNodeDataRequestChildren() {
    NodeDataRequest request = NodeDataRequest.createUniNodeDataRequest(VALID_UNINODE_HASH);
    request.setData(VALID_UNINODE_ENCODING);
    assertThat(request.getChildRequests())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            NodeDataRequest.createUniNodeValueDataRequest(VALID_UNINODE_VALUE_HASH),
            NodeDataRequest.createUniNodeDataRequest(VALID_UNINODE_CHILD_HASH)
    );
  }

  private NodeDataRequest serializeThenDeserialize(final NodeDataRequest request) {
    return NodeDataRequest.deserialize(NodeDataRequest.serialize(request));
  }

  private void assertRequestsEquals(final NodeDataRequest actual, final NodeDataRequest expected) {
    assertThat(actual.getRequestType()).isEqualTo(expected.getRequestType());
    assertThat(actual.getHash()).isEqualTo(expected.getHash());
    assertThat(actual.getData()).isEqualTo(expected.getData());
  }
}
