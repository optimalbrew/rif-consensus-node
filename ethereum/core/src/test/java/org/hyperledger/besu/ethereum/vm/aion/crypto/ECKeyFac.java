package org.hyperledger.besu.ethereum.vm.aion.crypto;

import org.hyperledger.besu.ethereum.vm.aion.crypto.ecdsa.ECKeySecp256k1;
//import org.hyperledger.besu.ethereum.vm.aion.crypto.ed25519.ECKeyEd25519;

/**
 * Factory class that generates key.
 *
 * @author jin, cleaned by yulong
 */
public class ECKeyFac {

    public enum ECKeyType {
        SECP256K1,
        ED25519
    }

    protected static ECKeyType type = ECKeyType.SECP256K1;//ECKeyType.ED25519;

    /**
     * Sets the signature algorithm type.
     *
     * @param t
     */
    public static void setType(ECKeyType t) {
        type = t;
    }

    private static class ECKeyFacHolder {
        private static final org.hyperledger.besu.ethereum.vm.aion.crypto.ECKeyFac INSTANCE = new org.hyperledger.besu.ethereum.vm.aion.crypto.ECKeyFac();
    }

    private ECKeyFac() {}

    /**
     * Returns the ECKey factory singleton instance.
     *
     * @return
     */
    public static org.hyperledger.besu.ethereum.vm.aion.crypto.ECKeyFac inst() {
        return ECKeyFacHolder.INSTANCE;
    }

    /**
     * Creates a random key pair.
     *
     * @return
     */
    public ECKey create() {
        switch (type) {
            case SECP256K1:
                return new ECKeySecp256k1();
            case ED25519:
                return null;//new ECKeyEd25519();
            default:
                throw new RuntimeException("ECKey type is not set!");
        }
    }

    /**
     * Recovers a key pair from the private key.
     *
     * @param pk
     * @return
     */
    public ECKey fromPrivate(byte[] pk) {
        switch (type) {
            case SECP256K1:
                return new ECKeySecp256k1().fromPrivate(pk);
            case ED25519:
                return null;//new ECKeyEd25519().fromPrivate(pk);
            default:
                throw new RuntimeException("ECKey type is not set!");
        }
    }
}
