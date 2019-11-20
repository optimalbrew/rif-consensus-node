package org.hyperledger.besu.ethereum.core;

import org.hyperledger.besu.ethereum.core.AccountState;
import org.hyperledger.besu.util.bytes.BytesValue;

public interface UnitrieAccountState extends AccountState {

    BytesValue getEncoded();
}