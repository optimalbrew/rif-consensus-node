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
package org.hyperledger.besu.ethereum.eth.sync.worldstate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderTestFixture;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.InMemoryStorageProvider;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorage;
import org.hyperledger.besu.services.tasks.Task;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PersistDataStepTest {

  private final WorldStateStorage worldStateStorage =
      new InMemoryStorageProvider().createWorldStateStorage();
  private final WorldDownloadState downloadState = mock(WorldDownloadState.class);

  private final Bytes rootNodeData = Bytes.of(1, 1, 1, 1);
  private final BlockHeader blockHeader =
      new BlockHeaderTestFixture().stateRoot(Hash.hash(rootNodeData)).buildHeader();

  private final PersistDataStep persistDataStep = new PersistDataStep(worldStateStorage);

  @Parameters(name = "use unitrie={0}")
  public static Object[] data() {
    // Use or not UniNodeDataRequests as opposed to classic NodeDataRequests
    return new Object[] {false, true};
  }

  @Parameter public boolean useClassicalRequest;

  @Test
  public void shouldPersistDataWhenPresent() {
    final List<Task<NodeDataRequest>> tasks =
        asList(
            createTaskWithData(1, 2, 3),
            createTaskWithData(4, 5, 6),
            createTaskWithData(7, 8, 9),
            createTaskWithData(10, 11, 12));

    final List<Task<NodeDataRequest>> result =
        persistDataStep.persist(tasks, blockHeader, downloadState);

    assertThat(result).isSameAs(tasks);
    assertDataPersisted(tasks);
  }

  @Test
  public void shouldSkipPersistingTasksWithNoData() {
    final Task<NodeDataRequest> withData = createTaskWithData(1, 2, 3);
    final StubTask withoutData = createTaskWithoutData(4, 5, 6);
    final List<Task<NodeDataRequest>> tasks = asList(withData, withoutData);

    final List<Task<NodeDataRequest>> result =
        persistDataStep.persist(tasks, blockHeader, downloadState);
    assertThat(result).isSameAs(tasks);

    assertThat(worldStateStorage.contains(withData.getData().getHash())).isTrue();
    assertThat(worldStateStorage.contains(withoutData.getData().getHash())).isFalse();
  }

  @Test
  public void shouldStoreRootNodeDataInDownloadStateInsteadOfPersisting() {
    final Task<NodeDataRequest> rootNode = createTaskWithData(rootNodeData);
    final List<Task<NodeDataRequest>> tasks = singletonList(rootNode);
    final List<Task<NodeDataRequest>> result =
        persistDataStep.persist(tasks, blockHeader, downloadState);
    assertThat(result).isSameAs(tasks);

    assertThat(worldStateStorage.contains(rootNode.getData().getHash())).isFalse();
    verify(downloadState).setRootNodeData(rootNode.getData().getData());
  }

  private Task<NodeDataRequest> createTaskWithData(final int... bytes) {
    return createTaskWithData(Bytes.of(bytes));
  }

  private Task<NodeDataRequest> createTaskWithData(final Bytes data) {
    final StubTask task = createTaskWithoutData(data);
    task.getData().setData(data);
    return task;
  }

  private StubTask createTaskWithoutData(final int... data) {
    return createTaskWithoutData(Bytes.of(data));
  }

  private StubTask createTaskWithoutData(final Bytes data) {
    final Hash hash = Hash.hash(data);
    final NodeDataRequest request =
        useClassicalRequest
            ? NodeDataRequest.createAccountDataRequest(hash)
            : NodeDataRequest.createUniNodeDataRequest(hash);
    return new StubTask(request);
  }

  private void assertDataPersisted(final List<Task<NodeDataRequest>> tasks) {
    tasks.forEach(
        task ->
            assertThat(worldStateStorage.getNodeData(task.getData().getHash()))
                .isEqualTo(Optional.of(task.getData().getData())));
  }
}
