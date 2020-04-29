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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AllUniNodesVisitorTest {

  @Mock private StoredUniNode leftStoredNode;

  @Mock private StoredUniNode rightStoredNode;

  @Mock private BranchUniNode branchNode;

  @Test
  public void visitBranch() {
    when(branchNode.getLeftChild()).thenReturn(leftStoredNode);
    when(branchNode.getRightChild()).thenReturn(rightStoredNode);

    new AllUniNodesVisitor(x -> {}).visit(branchNode);

    verify(leftStoredNode).unload();
    verify(rightStoredNode).unload();
  }
}
