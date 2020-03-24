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

import java.nio.ByteBuffer;
import java.util.Objects;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt24;
import org.hyperledger.besu.ethereum.unitrie.ints.UInt8;
import org.hyperledger.besu.ethereum.unitrie.ints.VarInt;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

/**
 * Encode or decode {@link UniNode}s.
 *
 * @author ppedemon
 */
class UniNodeEncoding {

  /**
   * Encode a {@link BranchUniNode}.
   *
   * @param node node to encode
   * @return encoded node, as mandated by RSKIP107
   */
  BytesValue encode(final BranchUniNode node) {
    byte flags = getFlags(node);
    int pathSize = node.getPath().length;
    BytesValue encodedPath = PathEncoding.encodePath(BytesValue.of(node.getPath()));

    VarInt childrenSize = VarInt.ZERO;
    if (!node.isLeaf()) {
      childrenSize = new VarInt(node.getChildrenSize());
    }

    int encodingSize =
        1
            + encodedPathSize(pathSize, encodedPath)
            + encodedChildSize(node.getLeftChild())
            + encodedChildSize(node.getRightChild())
            + (node.isLeaf() ? 0 : childrenSize.getSizeInBytes())
            + (node.getValueWrapper().isLong()
                ? Bytes32.SIZE + UInt24.BYTES
                : node.getValueLength().orElse(0));

    ByteBuffer buffer = ByteBuffer.allocate(encodingSize);
    buffer.put(flags);
    encodePath(pathSize, encodedPath, buffer);
    encodeChild(node.getLeftChild(), buffer);
    encodeChild(node.getRightChild(), buffer);

    if (!node.isLeaf()) {
      buffer.put(childrenSize.encode());
    }

    node.getValueWrapper().encodeTo(buffer);

    return BytesValue.wrap(buffer.array());
  }

  /**
   * Compute flags for the given node.
   *
   * @param node node to extract flags from
   * @return node flags (check RSKIP107)
   */
  private byte getFlags(final UniNode node) {
    byte flags = 0b01000000;

    if (node.getValueWrapper().isLong()) {
      flags = (byte) (flags | 0b00100000);
    }

    if (node.getPath().length > 0) {
      flags = (byte) (flags | 0b00010000);
    }

    if (isNotEmpty(node.getLeftChild())) {
      flags = (byte) (flags | 0b00001000);
    }

    if (isNotEmpty(node.getRightChild())) {
      flags = (byte) (flags | 0b00000100);
    }

    if (isEmbeddable(node.getLeftChild())) {
      flags = (byte) (flags | 0b00000010);
    }

    if (isEmbeddable(node.getRightChild())) {
      flags = (byte) (flags | 0b00000001);
    }

    return flags;
  }

  /**
   * Check whether the given node is empty.
   *
   * @param node node to check
   * @return whether the given node is empty
   */
  private boolean isNotEmpty(final UniNode node) {
    return Objects.nonNull(node) && node != NullUniNode.instance();
  }

  /**
   * Check if the given node is embeddable.
   *
   * @param node node to check
   * @return whether the given node is embeddable
   */
  private boolean isEmbeddable(final UniNode node) {
    return isNotEmpty(node) && !node.isReferencedByHash();
  }

  /**
   * Compute size in bytes of a node path.
   *
   * @param pathSizeInBits number of bits in node path
   * @param encodedPath encoded node path
   * @return size in bytes of node path encoding
   */
  private int encodedPathSize(final int pathSizeInBits, final BytesValue encodedPath) {
    if (pathSizeInBits == 0) {
      return 0;
    }

    int encodedSize = encodedPath.size();

    if (1 <= pathSizeInBits && pathSizeInBits <= 32) {
      return 1 + encodedSize;
    }
    if (160 <= pathSizeInBits && pathSizeInBits <= 382) {
      return 1 + encodedSize;
    }
    return 1 + VarInt.sizeOf(pathSizeInBits) + encodedSize;
  }

  /**
   * Dump path encoding to the given byte buffer.
   *
   * @param pathSizeInBits path size in bits
   * @param encodedPath encoded path
   * @param buffer destination bit buffer
   */
  private void encodePath(
      final int pathSizeInBits, final BytesValue encodedPath, final ByteBuffer buffer) {

    if (pathSizeInBits == 0) {
      return;
    }

    if (1 <= pathSizeInBits && pathSizeInBits <= 32) {
      buffer.put((byte) (pathSizeInBits - 1));
    } else if (160 <= pathSizeInBits && pathSizeInBits <= 382) {
      buffer.put((byte) (pathSizeInBits - 128));
    } else {
      buffer.put((byte) 255);
      buffer.put(new VarInt(pathSizeInBits).encode());
    }

    buffer.put(encodedPath.getArrayUnsafe());
  }

  /**
   * Determine size in bytes of the encoding for the given child node.
   *
   * @param child child node
   * @return size in bytes of child encoding
   */
  private int encodedChildSize(final UniNode child) {
    if (isNotEmpty(child)) {
      if (isEmbeddable(child)) {
        return 1 + child.getEncoding().length;
      }
      return Bytes32.SIZE;
    }
    return 0;
  }

  /**
   * Dump child encoding to the given byte buffer.
   *
   * @param child child to encode
   * @param buffer destination buffer
   */
  private void encodeChild(final UniNode child, final ByteBuffer buffer) {
    if (isNotEmpty(child)) {
      if (isEmbeddable(child)) {
        byte[] v = child.getEncoding();
        buffer.put(new UInt8(v.length).toByteArray());
        buffer.put(v);
      } else {
        buffer.put(child.getHash());
      }
    }
  }

  /**
   * Decode a {@link UniNode} from the given bytes value.
   *
   * @param value bytes value to decode from
   * @param nodeFactory {@link StoredUniNodeFactory} used to lazily solve nodes referenced by hash
   * @return decode node
   */
  UniNode decode(final BytesValue value, final StoredUniNodeFactory nodeFactory) {

    if (value.equals(UniTrie.NULL_UNINODE_ENCODING)) {
      return NullUniNode.instance();
    }

    ByteBuffer buffer = ByteBuffer.wrap(value.extractArray());
    return decode(buffer, nodeFactory);
  }

  /**
   * Decode a {@link UniNode} from the given bytee buffer.
   *
   * @param buffer byte buffer to decode from
   * @param nodeFactory {@link StoredUniNodeFactory} used to lazily solve nodes referenced by hash
   * @return decode node
   */
  private UniNode decode(final ByteBuffer buffer, final StoredUniNodeFactory nodeFactory) {

    byte flags = buffer.get();

    boolean hasLongValue = (flags & 0b00100000) == 0b00100000;
    boolean hasPath = (flags & 0b00010000) == 0b00010000;
    boolean hasLeftChild = (flags & 0b00001000) == 0b00001000;
    boolean hasRightChild = (flags & 0b00000100) == 0b00000100;
    boolean leftChildEmbedded = (flags & 0b00000010) == 0b00000010;
    boolean rightChildEmbedded = (flags & 0b00000001) == 0b00000001;

    byte[] path = new byte[0];
    if (hasPath) {
      path = decodePath(buffer);
    }

    UniNode leftChild = decodeChild(buffer, hasLeftChild, leftChildEmbedded, nodeFactory);
    UniNode rightChild = decodeChild(buffer, hasRightChild, rightChildEmbedded, nodeFactory);

    long childrenSize = -1;
    if (hasLeftChild || hasRightChild) {
      childrenSize = readVarInt(buffer).getValue();
    }

    ValueWrapper valueWrapper = ValueWrapper.decodeFrom(buffer, hasLongValue);

    if (buffer.hasRemaining()) {
      throw new IllegalArgumentException("The message had more data than expected");
    }

    return new BranchUniNode(path, valueWrapper, leftChild, rightChild, childrenSize);
  }

  /**
   * Decode a node path at the current position of the given buffer.
   *
   * @param buffer buffer to extract path from
   * @return extracted path
   */
  private byte[] decodePath(final ByteBuffer buffer) {
    int pathLengthInBits;
    int firstLengthByte = Byte.toUnsignedInt(buffer.get());

    if (0 <= firstLengthByte && firstLengthByte <= 31) {
      pathLengthInBits = firstLengthByte + 1;
    } else if (32 <= firstLengthByte && firstLengthByte <= 254) {
      pathLengthInBits = firstLengthByte + 128;
    } else {
      pathLengthInBits = (int) readVarInt(buffer).getValue();
    }

    int encodedLength = PathEncoding.encodedPathLength(pathLengthInBits);
    byte[] encodedPath = new byte[encodedLength];
    buffer.get(encodedPath);
    return PathEncoding.decodePath(BytesValue.wrap(encodedPath), pathLengthInBits).getArrayUnsafe();
  }

  /**
   * Read a {@link VarInt} from the current position of the given buffer.
   *
   * @param message buffer to read var int from
   * @return extracted {@link VarInt} instance
   */
  private VarInt readVarInt(final ByteBuffer message) {
    // read without touching the buffer position so when we read into bytes it contains the header
    int first = Byte.toUnsignedInt(message.get(message.position()));

    byte[] bytes;
    if (first < 253) {
      bytes = new byte[1];
    } else if (first == 253) {
      bytes = new byte[3];
    } else if (first == 254) {
      bytes = new byte[5];
    } else {
      bytes = new byte[9];
    }

    message.get(bytes);
    return new VarInt(bytes, 0);
  }

  /**
   * Decode a child from the current position of the given buffer.
   *
   * @param buffer buffer to decode from
   * @param hasChild whether we should actually decode a child (otherwise return a {@link
   *     NullUniNode})
   * @param isChildEmbedded whether child is embedded or we expect to find its hash
   * @param nodeFactory node factory for solving node hashes lazily
   * @return decode child node
   */
  private UniNode decodeChild(
      final ByteBuffer buffer,
      final boolean hasChild,
      final boolean isChildEmbedded,
      final StoredUniNodeFactory nodeFactory) {

    if (hasChild && isChildEmbedded) {
      byte[] lengthBytes = new byte[UInt8.BYTES];
      buffer.get(lengthBytes);
      UInt8 childLength = UInt8.fromBytes(lengthBytes);
      byte[] serializedNode = new byte[childLength.intValue()];
      buffer.get(serializedNode);
      return decode(ByteBuffer.wrap(serializedNode), nodeFactory);
    } else if (hasChild) {
      byte[] childHashBytes = new byte[Bytes32.SIZE];
      buffer.get(childHashBytes);
      return new StoredUniNode(childHashBytes, nodeFactory);
    }

    return NullUniNode.instance();
  }
}
