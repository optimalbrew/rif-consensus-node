package org.hyperledger.besu.ethereum.unitrie;

import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class UniTrieKeyValueTest {

    private static UniNode NO_RESULT = NullUniNode.instance();

    private final UniNodeFactory nodeFactory = new DefaultUniNodeFactory();

    @Test
    public void putWithEmptyKey() {
        UniNode trie = NullUniNode.instance();
        trie = trie.accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.EMPTY);

        assertThat(trie.accept(new GetVisitor(), BytesValue.EMPTY).getValue()).hasValue(BytesValue.of(1));
    }

    @Test
    public void putSingleValue() {
        UniNode trie = NullUniNode.instance();
        trie = trie.accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0)).getValue()).hasValue(BytesValue.of(1));
    }

    @Test
    public void putSingleValueTwice() {
        UniNode trie = NullUniNode.instance();
        UniNode trie0 = trie.accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 0));
        UniNode trie1 = trie0.accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 0));

        assertThat(trie0.accept(new GetVisitor(), BytesValue.of(0, 0)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie1.accept(new GetVisitor(), BytesValue.of(0, 0)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie0).isSameAs(trie1);
    }

    @Test
    public void putSingleAndReplace() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 0))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0)).getValue()).hasValue(BytesValue.of(2));
    }

    @Test
    public void splitLeftWithNoLeaf() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 0))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0)).getValue()).hasValue(BytesValue.of(2));
    }

    @Test
    public void splitRightWithNoLeaf() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0)).getValue()).hasValue(BytesValue.of(2));
    }

    @Test
    public void splitLeftWithNewLeaf() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 0, 0))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 1, 0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 0)).getValue()).hasValue(BytesValue.of(2));
    }

    @Test
    public void splitRightWithNewLeaf() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0, 0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 1)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0)).getValue()).hasValue(BytesValue.of(2));
    }

    @Test
    public void recurse() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0, 0))
                .accept(new PutVisitor(BytesValue.of(3), nodeFactory), BytesValue.of(0, 0, 0, 1, 0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 1)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0)).getValue()).hasValue(BytesValue.of(2));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0, 1, 0)).getValue()).hasValue(BytesValue.of(3));
    }

    @Test
    public void recurseShort() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0, 0))
                .accept(new PutVisitor(BytesValue.of(3), nodeFactory), BytesValue.of(0, 0, 0, 1));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 1)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0)).getValue()).hasValue(BytesValue.of(2));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0, 1)).getValue()).hasValue(BytesValue.of(3));
    }

    @Test
    public void recurseAndReplaceRoot() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0, 0))
                .accept(new PutVisitor(BytesValue.of(3), nodeFactory), BytesValue.of(0, 0, 0, 1, 0))
                .accept(new PutVisitor(BytesValue.of(4), nodeFactory), BytesValue.of(0));

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 1)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0)).getValue()).hasValue(BytesValue.of(2));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0, 1, 0)).getValue()).hasValue(BytesValue.of(3));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0)).getValue()).hasValue(BytesValue.of(4));
    }

    @Test
    public void recurseAndPutEmpty() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0, 0))
                .accept(new PutVisitor(BytesValue.of(3), nodeFactory), BytesValue.of(0, 0, 0, 1, 0))
                .accept(new PutVisitor(BytesValue.of(4), nodeFactory), BytesValue.EMPTY);

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0, 1, 0)).getValue()).hasValue(BytesValue.of(3));
        assertThat(trie.accept(new GetVisitor(), BytesValue.EMPTY).getValue()).hasValue(BytesValue.of(4));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 1)).getValue()).hasValue(BytesValue.of(1));
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0)).getValue()).hasValue(BytesValue.of(2));
    }

    @Test
    public void notFound() {
        UniNode trie = NullUniNode.instance();
        trie = trie
                .accept(new PutVisitor(BytesValue.of(1), nodeFactory), BytesValue.of(0, 1, 1))
                .accept(new PutVisitor(BytesValue.of(2), nodeFactory), BytesValue.of(0, 0, 0))
                .accept(new PutVisitor(BytesValue.of(3), nodeFactory), BytesValue.of(0, 0, 0, 1, 0))
                .accept(new PutVisitor(BytesValue.of(4), nodeFactory), BytesValue.EMPTY);

        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1))).isEqualTo(NO_RESULT);
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 1, 0))).isEqualTo(NO_RESULT);
        assertThat(trie.accept(new GetVisitor(), BytesValue.of(0, 0, 0, 1, 0, 1, 1))).isEqualTo(NO_RESULT);
    }

    @Test
    public void putAWholeLotOfStuff() {
        UniNode trie = NullUniNode.instance();
        for (int i = 0; i < 100; i++) {
            BytesValue value = BytesValue.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            BytesValue path = PathEncoding.decodePath(value, value.size() * 8);
            trie = trie.accept(new PutVisitor(value, nodeFactory), path);
        }

        for (int i = 0; i < 100; i++) {
            BytesValue value = BytesValue.wrap(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            BytesValue path = PathEncoding.decodePath(value, value.size() * 8);
            assertThat(trie.accept(new GetVisitor(), path).getValue()).hasValue(value);
        }
    }
}
