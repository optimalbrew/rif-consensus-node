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
package org.hyperledger.besu.ethereum.vm;

import org.hyperledger.besu.ethereum.core.BlockHeaderMock;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.worldstate.DefaultMutableWorldState;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties("_info")
public class GeneralStateTestCaseSpec extends AbstractGeneralStateTestCaseSpec {

  @JsonCreator
  public GeneralStateTestCaseSpec(
      @JsonProperty("env") final BlockHeaderMock blockHeader,
      @JsonProperty("pre") final WorldStateMock initialWorldState,
      @JsonProperty("post") final Map<String, List<PostSection>> postSection,
      @JsonProperty("transaction") final StateTestVersionedTransaction versionedTransaction) {
    super(blockHeader, initialWorldState, postSection, versionedTransaction);
  }

  @Override
  MutableWorldState runtimeWorldState(final WorldState initialWorldState) {
    return new DefaultMutableWorldState(initialWorldState);
  }

  @Override
  boolean shouldCheckRootHash() {
    return true;
  }
}
