/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.config;

import co.rsk.core.RskAddress;
import co.rsk.rpc.ModuleDescription;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.ethereum.config.Constants;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Account;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ajlopez on 3/3/2016.
 */
public class RskSystemProperties {
    /** while timeout period is lower than clean period it doesn't affect much since
    requests will be checked after a clean period.
     **/
    private static final int PD_DEFAULT_CLEAN_PERIOD = 15000; //miliseconds
    private static final int PD_DEFAULT_TIMEOUT_MESSAGE = PD_DEFAULT_CLEAN_PERIOD - 1; //miliseconds
    private static final int PD_DEFAULT_REFRESH_PERIOD = 60000; //miliseconds

    private static final String REGTEST_BLOCKCHAIN_CONFIG = "regtest";

    private static final String MINER_REWARD_ADDRESS_CONFIG = "miner.reward.address";
    private static final String MINER_COINBASE_SECRET_CONFIG = "miner.coinbase.secret";
    private static final int CHUNK_SIZE = 192;

    //TODO: REMOVE THIS WHEN THE LocalBLockTests starts working with REMASC
    private boolean remascEnabled = true;






    public GarbageCollectorConfig garbageCollectorConfig() {
        //From reference.conf
        /*enabled = false
        epochs = 3
        blocksPerEpoch = 20000*/
        return new GarbageCollectorConfig(false, 20000, 3);
        //return GarbageCollectorConfig.fromConfig(configFromFiles.getConfig("blockchain.gc"));
    }

    public int flushNumberOfBlocks() {
        return configFromFiles.hasPath("blockchain.flushNumberOfBlocks") && configFromFiles.getInt("blockchain.flushNumberOfBlocks") > 0 ?
                configFromFiles.getInt("blockchain.flushNumberOfBlocks") : 20;
    }

    public int soLingerTime() {
        return configFromFiles.getInt("rpc.providers.web.http.linger_time");

    }

    //TODO: REMOVE THIS WHEN THE LocalBLockTests starts working with REMASC
    public boolean isRemascEnabled() {
        return remascEnabled;
    }

    //TODO: REMOVE THIS WHEN THE LocalBLockTests starts working with REMASC
    public void setRemascEnabled(boolean remascEnabled) {
        this.remascEnabled = remascEnabled;
    }

    public long peerDiscoveryMessageTimeOut() {
        return getLong("peer.discovery.msg.timeout", PD_DEFAULT_TIMEOUT_MESSAGE);
    }

    public long peerDiscoveryRefreshPeriod() {
        long period = getLong("peer.discovery.refresh.period", PD_DEFAULT_REFRESH_PERIOD);

        return (period < PD_DEFAULT_REFRESH_PERIOD)? PD_DEFAULT_REFRESH_PERIOD : period;
    }

    public List<ModuleDescription> getRpcModules() {
        if (this.moduleDescriptions != null) {
            return this.moduleDescriptions;
        }

        List<ModuleDescription> modules = new ArrayList<>();

        if (!configFromFiles.hasPath("rpc.modules")) {
            return modules;
        }

        List<? extends ConfigObject> list = configFromFiles.getObjectList("rpc.modules");

        for (ConfigObject configObject : list) {
            Config configElement = configObject.toConfig();
            String name = configElement.getString("name");
            String version = configElement.getString("version");
            boolean enabled = configElement.getBoolean("enabled");
            List<String> enabledMethods = null;
            List<String> disabledMethods = null;

            if (configElement.hasPath("methods.enabled")) {
                enabledMethods = configElement.getStringList("methods.enabled");
            }

            if (configElement.hasPath("methods.disabled")) {
                disabledMethods = configElement.getStringList("methods.disabled");
            }

            modules.add(new ModuleDescription(name, version, enabled, enabledMethods, disabledMethods));
        }

        this.moduleDescriptions = modules;

        return modules;
    }

    public boolean hasMessageRecorderEnabled() {
        return getBoolean("messages.recorder.enabled",false);
    }

    public List<String> getMessageRecorderCommands() {
        if (!configFromFiles.hasPath("messages.recorder.commands")) {
            return new ArrayList<>();
        }

        return configFromFiles.getStringList("messages.recorder.commands");
    }

    public long getTargetGasLimit() {
        return getLong("targetgaslimit",6_800_000L);
    }

    public boolean getForceTargetGasLimit() {
        return getBoolean("forcegaslimit", true);
    }

    // Sync config properties
    public int getExpectedPeers() {
        return configFromFiles.getInt("sync.expectedPeers");
    }

    public int getTimeoutWaitingPeers() {
        return configFromFiles.getInt("sync.timeoutWaitingPeers");
    }

    public int getTimeoutWaitingRequest() {
        return configFromFiles.getInt("sync.timeoutWaitingRequest");
    }

    public int getExpirationTimePeerStatus() {
        return configFromFiles.getInt("sync.expirationTimePeerStatus");
    }

    public int getMaxSkeletonChunks() {
        return configFromFiles.getInt("sync.maxSkeletonChunks");
    }

    // its fixed, cannot be set by config file
    public int getChunkSize() {
        return CHUNK_SIZE;
    }

    public VmConfig getVmConfig() {
        return new VmConfig(vmTrace(), vmTraceInitStorageLimit(), dumpBlock(), dumpStyle());
    }

    public long peerDiscoveryCleanPeriod() {
        return PD_DEFAULT_CLEAN_PERIOD;
    }

    public int getPeerP2PPingInterval(){
        return configFromFiles.getInt("peer.p2p.pingInterval");
    }

    public Integer getGasPriceBump() {
        return configFromFiles.getInt("transaction.gasPriceBump");
    }

    public int getStatesCacheSize() {
        return configFromFiles.getInt("cache.states.max-elements");
    }

    public long getVmExecutionStackSize() {
        return configFromFiles.getBytes("vm.executionStackSize");
    }
}
