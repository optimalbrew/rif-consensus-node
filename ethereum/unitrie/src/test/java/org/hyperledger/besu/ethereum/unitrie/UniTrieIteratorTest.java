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

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.ethereum.trie.KeyValueMerkleStorage;
import org.hyperledger.besu.ethereum.trie.MerkleStorage;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;
import org.hyperledger.besu.util.uint.UInt256;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.NavigableSet;
import java.util.TreeSet;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UniTrieIteratorTest {

    private static final Bytes32 KEY_HASH1 =
            Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555555");
    private static final Bytes32 KEY_HASH2 =
            Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555556");
    private static final BytesValue PATH1 = PathEncoding.decodePath(KEY_HASH1, 256);
    private static final BytesValue PATH2 = PathEncoding.decodePath(KEY_HASH2, 256);

    private final MerkleStorage storage = new KeyValueMerkleStorage(new InMemoryKeyValueStorage());
    private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory(storage::get);
    private final UniTrieIterator.LeafHandler leafHandler = mock(UniTrieIterator.LeafHandler.class);
    private final UniTrieIterator iterator = new UniTrieIterator(leafHandler);

    @Test
    public void shouldCallLeafHandlerWhenRootNodeIsALeaf() {
        final UniNode leaf = nodeFactory.createLeaf(PATH1, ValueWrapper.fromValue(BytesValue.of(0)));
        leaf.accept(iterator, PATH1);
        verify(leafHandler).onLeaf(KEY_HASH1, leaf);
    }

    @Test
    public void shouldNotNotifyLeafHandlerOfNullNodes() {
        NullUniNode.instance().accept(iterator, PATH1);
        verifyZeroInteractions(leafHandler);
    }

    @Test
    public void shouldVisitEachChildOfABranchNode() {
        when(leafHandler.onLeaf(any(Bytes32.class), any(UniNode.class))).thenReturn(UniTrieIterator.State.CONTINUE);
        final UniNode root = NullUniNode.instance()
                .accept(new PutVisitor(BytesValue.of(5), nodeFactory), PATH1)
                .accept(new PutVisitor(BytesValue.of(6), nodeFactory), PATH2);
        root.accept(iterator, PATH1);

        final InOrder inOrder = inOrder(leafHandler);
        inOrder.verify(leafHandler).onLeaf(eq(KEY_HASH1), any(UniNode.class));
        inOrder.verify(leafHandler).onLeaf(eq(KEY_HASH2), any(UniNode.class));
        verifyNoMoreInteractions(leafHandler);
    }

    @Test
    public void shouldStopIteratingChildrenOfBranchWhenLeafHandlerReturnsStop() {
        when(leafHandler.onLeaf(any(Bytes32.class), any(UniNode.class))).thenReturn(UniTrieIterator.State.STOP);
        final UniNode root = NullUniNode.instance()
                        .accept(new PutVisitor(BytesValue.of(5), nodeFactory), PATH1)
                        .accept(new PutVisitor(BytesValue.of(5), nodeFactory), PATH2);
        root.accept(iterator, PATH1);

        verify(leafHandler).onLeaf(eq(KEY_HASH1), any(UniNode.class));
        verifyNoMoreInteractions(leafHandler);
    }

    @Test
    @SuppressWarnings("unused")
    public void shouldIterateArbitraryStructureAccurately() {
        UniNode root = NullUniNode.instance();
        final NavigableSet<Bytes32> expectedKeyHashes = new TreeSet<>();
        Bytes32 startAtHash = Bytes32.ZERO;
        Bytes32 stopAtHash = Bytes32.ZERO;

        final int totalNodes = 500;
        final int startNodeNumber = 25;
        final int stopNodeNumber = 437;
        LinearCongruentialGenerator g = new LinearCongruentialGenerator();
        for (int i = 0; i < totalNodes; i++) {
            final Bytes32 keyHash = Hash.keccak256(UInt256.of(Math.abs(g.next())).getBytes());
            root = root.accept(new PutVisitor(BytesValue.of(1), nodeFactory), PathEncoding.decodePath(keyHash, 256));
            expectedKeyHashes.add(keyHash);
            if (i == startNodeNumber) {
                startAtHash = keyHash;
            } else if (i == stopNodeNumber) {
                stopAtHash = keyHash;
            }
        }

        final Bytes32 actualStopAtHash = stopAtHash.compareTo(startAtHash) >= 0 ? stopAtHash : startAtHash;
        when(leafHandler.onLeaf(any(Bytes32.class), any(UniNode.class))).thenReturn(UniTrieIterator.State.CONTINUE);
        when(leafHandler.onLeaf(eq(actualStopAtHash), any(UniNode.class))).thenReturn(UniTrieIterator.State.STOP);
        root.accept(iterator, PathEncoding.decodePath(startAtHash, 256));

        final InOrder inOrder = inOrder(leafHandler);
        expectedKeyHashes
                .subSet(startAtHash, true, actualStopAtHash, true)
                .forEach(keyHash -> inOrder.verify(leafHandler).onLeaf(eq(keyHash), any(UniNode.class)));
        verifyNoMoreInteractions(leafHandler);
    }

    private static class LinearCongruentialGenerator {
        final int m = 34783241;
        final int a = 6748319;
        final int c = 8736428;
        int seed = 984732957;

        int next() {
            seed = (a*seed + c) % m;
            return seed;
        }
    }
}
