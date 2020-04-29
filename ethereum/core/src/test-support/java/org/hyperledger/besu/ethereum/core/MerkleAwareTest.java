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
package org.hyperledger.besu.ethereum.core;

import org.hyperledger.besu.ethereum.merkleutils.ClassicMerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.UniTrieMerkleAwareProvider;

import java.util.Arrays;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Base class for tests parametrized with a {@link MerkleAwareProvider}.
 *
 * <p>Tests parameterized on merkle aware provider should extend this class and access the merkle
 * aware probvider by means of the getter method.
 *
 * @author ppedemon
 */
@RunWith(Parameterized.class)
public class MerkleAwareTest {

  @Parameters(name = "{0}")
  public static Iterable<MerkleAwareProvider> data() {
    return Arrays.asList(new ClassicMerkleAwareProvider(), new UniTrieMerkleAwareProvider());
  }

  @Parameter public MerkleAwareProvider merkleAwareProvider;

  public MerkleAwareProvider getMerkleAwareProvider() {
    return merkleAwareProvider;
  }
}
