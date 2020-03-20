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
import java.util.Optional;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Empty Unitrie node.
 *
 * @author ppedemon
 */
public class NullUniNode implements UniNode {

  private static byte[] EMPTY_BYES = new byte[0];
  private static NullUniNode INSTANCE = new NullUniNode();

  private NullUniNode() {
    super();
  }

  public static NullUniNode instance() {
    return INSTANCE;
  }

  @Override
  public byte[] getPath() {
    return EMPTY_BYES;
  }

  @Override
  public ValueWrapper getValueWrapper() {
    return ValueWrapper.EMPTY;
  }

  @Override
  public Optional<byte[]> getValue(final DataLoader loader) {
    return Optional.empty();
  }

  @Override
  public Optional<byte[]> getValueHash() {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getValueLength() {
    return Optional.empty();
  }

  @Override
  public UniNode getLeftChild() {
    return null;
  }

  @Override
  public UniNode getRightChild() {
    return null;
  }

  @Override
  public String print(final int indent) {
    return String.format("%s[null]", Strings.repeat(" ", indent));
  }

  @Override
  public String toString() {
    return print(0);
  }

  @Override
  public UniNode accept(final UniPathVisitor visitor, final BytesValue path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final UniNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public byte[] getEncoding() {
    return NULL_UNINODE_ENCODING;
  }

  @Override
  public byte[] getHash() {
    return NULL_UNINODE_HASH;
  }

  @Override
  public boolean isReferencedByHash() {
    return false;
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void markDirty() {}
}
