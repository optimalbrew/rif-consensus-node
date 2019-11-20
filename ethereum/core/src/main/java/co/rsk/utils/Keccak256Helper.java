package co.rsk.utils;
import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.util.bytes.Bytes32;
import org.hyperledger.besu.util.bytes.BytesValue;

public class Keccak256Helper {
    public static final int DEFAULT_SIZE = 256;
    public static final int DEFAULT_SIZE_BYTES = DEFAULT_SIZE / 8;


    public static byte[] keccak256(String message) {
        return Hash.keccak256(Bytes32.fromHexString(message)).extractArray();
       // return keccak256(Hex.decode(message), new KeccakDigest(DEFAULT_SIZE), true);
    }

    public static Bytes32 keccak256(byte[] message) {
        return Hash.keccak256(message);
        //return keccak256(message, new KeccakDigest(DEFAULT_SIZE), true);
    }

    public static Bytes32 keccak256(BytesValue message) {
        return Hash.keccak256(message);
        //return keccak256(message, new KeccakDigest(DEFAULT_SIZE), true);
    }


}
