/*
 * This file is part of RskJ
 * Copyright (C) 2019 RSK Labs Ltd.
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

package co.rsk.trie;

import co.rsk.core.Coin;
import co.rsk.crypto.Keccak256;
import co.rsk.utils.RLPUtils;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;
import org.hyperledger.besu.util.bytes.BytesValue;

import javax.annotation.Nullable;
import java.math.BigInteger;

/**
 * This class holds the Orchid account state encoding logic
 */
@SuppressWarnings("squid:S2384") // this class is left for TrieConverter, we don't need to copy the byte[] arguments
public class OrchidAccountState {

    private static final Keccak256 EMPTY_DATA_HASH = new Keccak256(Hash.EMPTY);

    private BytesValue rlpEncoded;

    private BigInteger nonce;
    private Coin balance;
    private Keccak256 stateRoot = new Keccak256(Hash.EMPTY_TRIE_HASH);
    private Keccak256 codeHash = EMPTY_DATA_HASH;

    public OrchidAccountState(BytesValue rlpData) {

        RLPInput rlpInput = RLP.input(rlpData); //RLPList items = (RLPList) RLP.decode2(rlpData).get(0);

        if(!rlpInput.nextIsList()){
            throw new NullPointerException();
            //TODO: DO SOMETHING LOGICAL INSTEAD OF THROWING EXCEPTION
        }
        rlpInput.enterList();

        this.nonce = rlpInput.readBigIntegerScalar();//this.nonce = items.get(0).getRLPData() == null ? BigInteger.ZERO : new BigInteger(1, items.get(0).getRLPData());
        this.balance = RLPUtils.parseCoin(rlpInput.readBigIntegerScalar());//this.balance = RLP.parseCoin(items.get(1).getRLPData());
        this.stateRoot = new Keccak256(rlpInput.readBytes32());//items.get(2).getRLPData();
        this.codeHash = new Keccak256(rlpInput.readBytes32());//items.get(3).getRLPData();
        this.rlpEncoded = rlpData;
    }


    public OrchidAccountState(BigInteger nonce, Coin balance) {
        this.nonce = nonce;
        this.balance = balance;
    }

    public void setStateRoot(Keccak256 stateRoot) {
        rlpEncoded = null;
        this.stateRoot = stateRoot;
    }

    public void setCodeHash(Keccak256 codeHash) {
        rlpEncoded = null;
        this.codeHash = codeHash;
    }

    public void writeTo(final RLPOutput out) {
        out.startList();
        out.writeBigIntegerScalar(this.nonce);
        out.writeBigIntegerScalar(this.balance == null ? BigInteger.ZERO : this.balance.asBigInteger());
        out.writeBytesValue(this.stateRoot.getBytes());
        out.writeBytesValue(this.codeHash.getBytes());
        out.endList();
    }

    public BytesValue getEncoded() {

        if (rlpEncoded == null) {
            //BytesValue nonce = RLPUtils.encodeBigInteger(this.nonce);//RLP.encodeBigInteger(this.nonce);
            //BytesValue balance = RLPUtils.encodeCoin(this.balance);
            //BytesValue stateRoot = RLP.encodeOne(this.stateRoot.getBytes());
            //BytesValue codeHash = RLP.encodeOne(this.codeHash.getBytes());
            //RLP.encodeList(nonce, balance, stateRoot, codeHash);
            this.rlpEncoded = RLP.encode(this::writeTo);
        }

        return rlpEncoded;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public Coin getBalance() {
        return balance;
    }

    public Keccak256 getStateRoot() {
        return new Keccak256(stateRoot.getBytes().copy());
        //return Arrays.copyOf(stateRoot, stateRoot.length);
    }

    public Keccak256 getCodeHash() {
        return new Keccak256(codeHash.getBytes().copy());
        //return Arrays.copyOf(codeHash, codeHash.length);
    }
}