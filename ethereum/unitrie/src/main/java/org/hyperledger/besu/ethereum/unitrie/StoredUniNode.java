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

import com.google.common.base.Strings;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.ethereum.unitrie.ints.VarInt;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link UniNode} modeling a reference by hash to storage, where the actual node can be found.
 *
 * @author ppedemon
 */
public class StoredUniNode implements UniNode {

    private final Bytes32 hash;
    private final StoredUniNodeFactory nodeFactory;
    private UniNode loadedNode;

    StoredUniNode(final Bytes32 hash, final StoredUniNodeFactory nodeFactory) {
        this.hash = hash;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public BytesValue getPath() {
        return load().getPath();
    }

    @Override
    public ValueWrapper getValueWrapper() {
        return load().getValueWrapper();
    }

    @Override
    public Optional<BytesValue> getValue() {
        return load().getValue();
    }

    @Override
    public Optional<Bytes32> getValueHash() {
        return load().getValueHash();
    }

    @Override
    public Optional<UInt24> getValueLength() {
        return load().getValueLength();
    }

    @Override
    public UniNode getLeftChild() {
        return load().getLeftChild();
    }

    @Override
    public UniNode getRightChild() {
        return load().getRightChild();
    }

    @Override
    public VarInt getChildrenSize() {
        return load().getChildrenSize();
    }

    @Override
    public long intrinsicSize() {
        return load().intrinsicSize();
    }

    @Override
    public String print(final int indent) {
        if (Objects.isNull(loadedNode)) {
            return String.format("%sStored → %s", Strings.repeat(" ", indent), hash);
        } else {
            return loadedNode.print(indent);
        }
    }

    @Override
    public String toString() {
        return print(0);
    }

    @Override
    public UniNode accept(final UniPathVisitor visitor, final BytesValue path) {
        return load().accept(visitor, path);
    }

    @Override
    public void accept(final UniNodeVisitor visitor) {
        load().accept(visitor);
    }

    @Override
    public BytesValue getEncoding() {
        return load().getEncoding();
    }

    @Override
    public Bytes32 getHash() {
        return hash;
    }

    @Override
    public boolean isReferencedByHash() {
        return true;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void markDirty() {
        throw new IllegalStateException(
                "A stored UniNode cannot ever be dirty since it's loaded from storage");
    }

    @Override
    public void unload() {
        loadedNode = null;
    }

    private UniNode load() {
        if (loadedNode == null) {
            loadedNode = nodeFactory.retrieve(hash).orElseThrow(() ->
                    new IllegalStateException("Unable to load UniNode for hash: " + hash)
            );
        }
        return loadedNode;
    }
}
