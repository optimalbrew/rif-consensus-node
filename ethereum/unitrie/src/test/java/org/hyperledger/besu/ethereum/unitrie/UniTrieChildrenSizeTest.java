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

import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Optional;

import org.junit.Test;

public class UniTrieChildrenSizeTest {

  private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory(__ -> Optional.empty());

  @Test
  public void childrenSizeEmpty() {
    UniNode trie = NullUniNode.instance();
    assertThat(trie.getChildrenSize().getValue()).isEqualTo(0);
  }

  @Test
  public void childrenSizeShort() {
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(BytesValue.of(0), nodeFactory), BytesValue.of(0))
            .accept(new PutVisitor(BytesValue.wrap(new byte[32]), nodeFactory), BytesValue.of(1));
    assertThat(trie.getChildrenSize().getValue()).isEqualTo(35);
  }

  @Test
  public void childrenSizeLong() {
    UniNode trie =
        NullUniNode.instance()
            .accept(new PutVisitor(BytesValue.of(0), nodeFactory), BytesValue.of(0))
            .accept(new PutVisitor(BytesValue.wrap(new byte[33]), nodeFactory), BytesValue.of(1));
    assertThat(trie.getChildrenSize().getValue()).isEqualTo(71);
  }
}
