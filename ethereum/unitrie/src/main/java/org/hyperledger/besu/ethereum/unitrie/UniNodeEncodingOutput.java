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

/**
 * Class modeling {@link UniNodeEncoding} output when loading a node from storage.
 *
 * @author ppedemon
 */
class UniNodeEncodingOutput {

  private final byte[] path;
  private final ValueWrapper valueWrapper;
  private final UniNode leftChild;
  private final UniNode rightChild;
  private final long childrenSize;
  private final byte[] encoding;

  public UniNodeEncodingOutput(
      final byte[] path,
      final ValueWrapper valueWrapper,
      final UniNode leftChild,
      final UniNode rightChild,
      final long childrenSize,
      final byte[] encoding) {

    this.path = path;
    this.valueWrapper = valueWrapper;
    this.leftChild = leftChild;
    this.rightChild = rightChild;
    this.childrenSize = childrenSize;
    this.encoding = encoding;
  }

  public byte[] getPath() {
    return path;
  }

  public ValueWrapper getValueWrapper() {
    return valueWrapper;
  }

  public UniNode getLeftChild() {
    return leftChild;
  }

  public UniNode getRightChild() {
    return rightChild;
  }

  public long getChildrenSize() {
    return childrenSize;
  }

  public byte[] getEncoding() {
    return encoding;
  }
}
