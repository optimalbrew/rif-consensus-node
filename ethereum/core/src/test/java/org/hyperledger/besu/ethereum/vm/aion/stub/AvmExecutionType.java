package org.hyperledger.besu.ethereum.vm.aion.stub;

public enum AvmExecutionType {
    ASSUME_MAINCHAIN,
    ASSUME_SIDECHAIN,
    ASSUME_DEEP_SIDECHAIN,
    SWITCHING_MAINCHAIN,
    MINING,
    ETH_CALL;
}
