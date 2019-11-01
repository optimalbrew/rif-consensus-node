package co.rsk.utils;

import co.rsk.core.Coin;
import org.bouncycastle.util.BigIntegers;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.util.bytes.BytesValue;

import javax.annotation.Nullable;
import java.math.BigInteger;

public class RLPUtils {

    public static BytesValue encodeBigInteger(final BigInteger number){
        if(BigInteger.ZERO.equals(number)){
            return RLP.encodeOne(BytesValue.of(0x00));
        }
        return RLP.encodeOne(BytesValue.wrap(BigIntegers.asUnsignedByteArray(number)));
    }

    public static BytesValue encodeCoin(@Nullable final Coin coin) {
        if (coin == null) {
            return encodeBigInteger(BigInteger.ZERO);
        }

        return encodeBigInteger(coin.asBigInteger());
    }

    public static Coin parseCoin(@Nullable final BigInteger bytes) {
        if (bytes == null || BigInteger.ZERO.equals(bytes)) {
            return Coin.ZERO;
        } else {
            return new Coin(bytes);
        }
    }
}
