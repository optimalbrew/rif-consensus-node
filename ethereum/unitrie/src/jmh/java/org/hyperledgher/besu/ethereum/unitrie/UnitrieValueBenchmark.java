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

package org.hyperledgher.besu.ethereum.unitrie;

import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.ethereum.unitrie.AllUniNodesVisitor;
import org.hyperledger.besu.ethereum.unitrie.DataLoader;
import org.hyperledger.besu.ethereum.unitrie.DefaultUniNodeFactory;
import org.hyperledger.besu.ethereum.unitrie.NullUniNode;
import org.hyperledger.besu.ethereum.unitrie.PutVisitor;
import org.hyperledger.besu.ethereum.unitrie.UniNode;
import org.hyperledger.besu.ethereum.unitrie.UniNodeFactory;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import java.util.Random;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
public class UnitrieValueBenchmark {

  private Random random;
  private UniNodeFactory nodeFactory;
  private MerkleStorage storage;
  private DataLoader loader;

  @Param({"64", "32", "10"})
  public int valueLength;

  private UniNode root;

  @Setup
  public void prepare() {
    random = new Random();
    storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
    nodeFactory = new DefaultUniNodeFactory();
    loader = storage::get;

    root =
        keys()
            .limit(1000)
            .<UniNode>reduce(
                NullUniNode.instance(),
                (n, k) -> n.accept(new PutVisitor(randomBytes(valueLength), nodeFactory), k),
                (n, __) -> n);
  }

  @Benchmark
  public void getValue(final Blackhole blackhole) {
    root.accept(new AllUniNodesVisitor(node -> blackhole.consume(node.getValue(loader))));
  }

  private Stream<Bytes> keys() {
    return Stream.generate(() -> Bytes.wrap(randomBytes(32)));
  }

  private byte[] randomBytes(final int length) {
    byte[] key = new byte[length];
    random.nextBytes(key);
    return key;
  }
}
