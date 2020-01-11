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

import java.util.Collection;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.worldstate.DefaultMutableWorldState;
import org.hyperledger.besu.testutil.JsonTestParameters;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** The VM operation testing framework entry point. */
@RunWith(Parameterized.class)
public class VMReferenceTest extends AbstractVMReferenceTest {

  @Parameters(name = "Name: {0}")
  public static Collection<Object[]> getTestParametersForConfig() throws Exception {
    return JsonTestParameters.create(VMReferenceTestCaseSpec.class)
        .blacklist(BLACKLISTED_TESTS)
        .generate(TEST_CONFIG_FILE_DIR_PATHS);
  }

  public VMReferenceTest(
      final String name, final VMReferenceTestCaseSpec spec, final boolean runTest) {
   super(name, spec, runTest);
  }

  @Override
  MutableWorldState runtimeWorldState(final WorldState initialWorldState) {
    return new DefaultMutableWorldState(initialWorldState);
  }
}
