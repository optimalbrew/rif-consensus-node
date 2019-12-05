package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.crypto.Hash;
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

public class UnitrieIteratorTest {

    private static final Bytes32 KEY_HASH1 =
            Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555555");
    private static final Bytes32 KEY_HASH2 =
            Bytes32.fromHexString("0x5555555555555555555555555555555555555555555555555555555555555556");
    private static final BytesValue PATH1 = PathEncoding.decodePath(KEY_HASH1, 256);
    private static final BytesValue PATH2 = PathEncoding.decodePath(KEY_HASH2, 256);

    private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory();
    private final UnitrieIterator.LeafHandler leafHandler = mock(UnitrieIterator.LeafHandler.class);
    private final UnitrieIterator iterator = new UnitrieIterator(leafHandler);

    @Test
    public void shouldCallLeafHandlerWhenRootNodeIsALeaf() {
        final UniNode leaf = nodeFactory.createLeaf(PATH1, BytesValue.of(0));
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
        when(leafHandler.onLeaf(any(Bytes32.class), any(UniNode.class))).thenReturn(UnitrieIterator.State.CONTINUE);
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
        when(leafHandler.onLeaf(any(Bytes32.class), any(UniNode.class))).thenReturn(UnitrieIterator.State.STOP);
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
        when(leafHandler.onLeaf(any(Bytes32.class), any(UniNode.class))).thenReturn(UnitrieIterator.State.CONTINUE);
        when(leafHandler.onLeaf(eq(actualStopAtHash), any(UniNode.class))).thenReturn(UnitrieIterator.State.STOP);
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
