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
package org.hyperledger.besu.ethereum.unitrie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.unitrie.ByteTestUtils.bytes;

import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

public class UniTrieChildrenSizeTest {

  private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory();

  @Test
  public void childrenSizeEmpty() {
    UniNode trie = NullUniNode.instance();
    assertThat(trie.getChildrenSize()).isEqualTo(0);
  }

  @Test
  public void childrenSizeShort() {
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(bytes(0), nodeFactory), BytesValue.of(0))
            .accept(new PutVisitor(new byte[64], nodeFactory), BytesValue.of(1));
    assertThat(trie.getChildrenSize()).isEqualTo(67);
  }

  @Test
  public void childrenSizeLong() {
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(bytes(0), nodeFactory), BytesValue.of(0))
            .accept(new PutVisitor(new byte[65], nodeFactory), BytesValue.of(1));
    assertThat(trie.getChildrenSize()).isEqualTo(103);
  }
}