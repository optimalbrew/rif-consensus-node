package org.hyperledger.besu.ethereum.vm.aion.rlp;

import org.hyperledger.besu.ethereum.vm.aion.util.bytes.ByteUtil;

import java.util.ArrayList;

/**
 * @author Roman Mandeleil
 * @since 21.04.14
 */
public class RLPList extends ArrayList<org.hyperledger.besu.ethereum.vm.aion.rlp.RLPElement> implements org.hyperledger.besu.ethereum.vm.aion.rlp.RLPElement {

    private static final long serialVersionUID = -2855280911054117106L;

    byte[] rlpData;

    public void setRLPData(byte[] rlpData) {
        this.rlpData = rlpData;
    }

    public byte[] getRLPData() {
        return rlpData;
    }

    public static void recursivePrint(org.hyperledger.besu.ethereum.vm.aion.rlp.RLPElement element) {

        if (element == null) {
            throw new RuntimeException("RLPElement object can't be null");
        }
        if (element instanceof org.hyperledger.besu.ethereum.vm.aion.rlp.RLPList) {
            org.hyperledger.besu.ethereum.vm.aion.rlp.RLPList rlpList = (org.hyperledger.besu.ethereum.vm.aion.rlp.RLPList) element;
            System.out.print("[");
            for (org.hyperledger.besu.ethereum.vm.aion.rlp.RLPElement singleElement : rlpList) {
                recursivePrint(singleElement);
            }
            System.out.print("]");
        } else {
            String hex = ByteUtil.toHexString(element.getRLPData());
            System.out.print(hex + ", ");
        }
    }
}
