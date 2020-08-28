package org.hyperledger.besu.ethereum.vm.aion.base;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.aion.types.AionAddress;
import org.hyperledger.besu.ethereum.vm.aion.crypto.AddressSpecs;
import org.hyperledger.besu.ethereum.vm.aion.crypto.HashUtil;
import org.hyperledger.besu.ethereum.vm.aion.rlp.RLP;


public class TxUtil {

    private TxUtil() {
        throw new AssertionError("TxUtil may not be instantiated");
    }

    /** Calculates the address as per the QA2 definitions */
    public static AionAddress calculateContractAddress(byte[] addr, BigInteger nonce) {
        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.put(AddressSpecs.A0_IDENTIFIER);

        byte[] encSender = RLP.encodeElement(addr);
        byte[] encNonce = RLP.encodeBigInteger(nonce);

        buf.put(HashUtil.h256(RLP.encodeList(encSender, encNonce)), 1, 31);
        return new AionAddress(buf.array());
    }
}
