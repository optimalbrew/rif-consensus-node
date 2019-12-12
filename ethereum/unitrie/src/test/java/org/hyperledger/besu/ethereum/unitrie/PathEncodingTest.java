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

import org.hyperledger.besu.util.bytes.BytesValue;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@RunWith(Enclosed.class)
public class PathEncodingTest {

    /**
     * Test data for parameterized tests,
     */
    private static class TestData {
        BytesValue encoded;
        BytesValue path;
        int pathLength;

        TestData(final byte[] path, final byte[] encoded, final int pathLength) {
            this.path = BytesValue.of(path);
            this.encoded = BytesValue.of(encoded);
            this.pathLength = pathLength;
        }

        @Override
        public String toString() {
            return String.format("Encoded = %s ↔ path = %s (length = %d)", encoded, path, pathLength);
        }
    }

    /**
     * Parameterized encoding/decoding tests.
     */
    @RunWith(Parameterized.class)
    public static class ParameterizedTests {

        @Parameterized.Parameters(name="{index}: {0}")
        public static Collection<TestData> testData() {
            return Arrays.asList(
                    new TestData(
                            new byte[]{0x00, 0x01, 0x01},
                            new byte[]{0x60},
                            3),
                    new TestData(
                            new byte[]{0x00, 0x01, 0x01, 0x00, 0x01, 0x01, 0x00, 0x01},
                            new byte[]{0x6d},
                            8),
                    new TestData(
                            new byte[]{0x00, 0x01, 0x01, 0x00, 0x01, 0x01, 0x00, 0x01, 0x01},
                            new byte[]{0x6d, (byte)0x80},
                            9),
                    new TestData(
                            new byte[]{0x00, 0x01, 0x01, 0x00, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01},
                            new byte[]{0x6d, 0x50},
                            12)
            );
        }

        @Parameterized.Parameter
        public TestData testData;

        @Test
        public void encodeBinaryPath() {
            assertThat(PathEncoding.encodePath(testData.path)).isEqualTo(testData.encoded);
        }

        @Test
        public void decodeBinaryPath() {
            assertThat(PathEncoding.decodePath(testData.encoded, testData.pathLength)).isEqualTo(testData.path);
        }
    }

    /**
     * Test resilience to {@code null} inputs. Non parameterized.
     */
    public static class NullTests {
        @Test
        public void encodeNullBinaryPath() {
            assertThatNullPointerException().isThrownBy(() -> PathEncoding.encodePath(null));
        }

        @Test
        public void decodeNullBinaryPath() {
            assertThatNullPointerException().isThrownBy(() -> PathEncoding.decodePath(null, 0));
        }
    }
}
