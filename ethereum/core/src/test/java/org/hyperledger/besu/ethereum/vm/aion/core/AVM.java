package org.hyperledger.besu.ethereum.vm.aion.core;

import org.aion.avm.core.*;
import org.aion.types.Transaction;
import org.hyperledger.besu.ethereum.vm.aion.AvmFutureResult;
import org.hyperledger.besu.ethereum.vm.aion.internal.AionCapabilities;
import org.hyperledger.besu.ethereum.vm.aion.stub.AvmExecutionType;
import org.hyperledger.besu.ethereum.vm.aion.stub.IAvmExternalState;

public final class AVM {

    private final AvmImpl avm;

    private AVM(AvmImpl avm) {
        this.avm = avm;
    }

    /**
     * Constructs a new Avm instance and starts it up.
     *
     * @return A new AVM.
     */
    public static AVM createAndInitializeNewAvm() {
        return new AVM(CommonAvmFactory.buildAvmInstanceForConfiguration(new AionCapabilities(), new AvmConfiguration()));
    }

    /**
     * Executes the transactions and returns future results.
     *
     * @param externalState The current state of the world.
     * @param transactions The transactions to execute.
     * @param avmExecutionType The execution type.
     * @param cachedBlockNumber The cached block number.
     * @return the execution results.
     */
    public AvmFutureResult[] run(IAvmExternalState externalState, Transaction[] transactions, AvmExecutionType avmExecutionType, long cachedBlockNumber) {
        ExecutionType executionType = toExecutionType(avmExecutionType);
        FutureResult[] results = this.avm.run(((IExternalState) externalState), transactions, executionType, cachedBlockNumber);
        return wrapAvmFutureResults(results);
    }

    /**
     * Shuts down the AVM. Once the AVM is shut down, the {@code run()} method can no longer be
     * invoked!
     */
    public void shutdown() {
        this.avm.shutdown();
    }

    private static ExecutionType toExecutionType(AvmExecutionType executionType) {
        switch (executionType) {
            case MINING: return ExecutionType.MINING;
            case ETH_CALL: return ExecutionType.ETH_CALL;
            case ASSUME_MAINCHAIN: return ExecutionType.ASSUME_MAINCHAIN;
            case ASSUME_SIDECHAIN: return ExecutionType.ASSUME_SIDECHAIN;
            case SWITCHING_MAINCHAIN: return ExecutionType.SWITCHING_MAINCHAIN;
            case ASSUME_DEEP_SIDECHAIN: return ExecutionType.ASSUME_DEEP_SIDECHAIN;
            default: throw new IllegalArgumentException("Unknown execution type: " + executionType);
        }
    }

    private static AvmFutureResult[] wrapAvmFutureResults(FutureResult[] results) {
        if (results == null) {
            throw new NullPointerException("Cannot convert null results!");
        }

        AvmFutureResult[] avmResults = new AvmFutureResult[results.length];

        int index = 0;
        for (FutureResult result : results) {
            avmResults[index] = AvmFutureResult.wrap(result);
            index++;
        }

        return avmResults;
    }

}
