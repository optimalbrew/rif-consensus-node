package org.hyperledger.besu.ethereum.vm;

import org.aion.types.AionAddress;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.vm.aion.core.AVM;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AVMTest {

    private AVM avm;

    private long energyPrice = 10_000_000_000L;

    @Before
    public void setup() {
        this.avm = AVM.createAndInitializeNewAvm();
    }//operationRegistry, gasCalculator

    @Test
    public void testDeployContract() {
        //TransactionTypeRule.allowAVMContractTransaction();
        byte[] jar = getJarBytes(AvmVersion.VERSION_1);
        AionTransaction transaction =
                AionTransaction.create(
                        deployerKey,
                        new byte[0],
                        null,
                        new byte[0],
                        jar,
                        5_000_000,
                        energyPrice,
                        TransactionTypes.AVM_CREATE_CODE, null);

        MiningBlock block =
                this.blockchain.createNewMiningBlock(
                        this.blockchain.getBestBlock(),
                        Collections.singletonList(transaction),
                        false);
        Pair<ImportResult, AionBlockSummary> connectResult =
                this.blockchain.tryToConnectAndFetchSummary(block);
        AionTxReceipt receipt = connectResult.getRight().getReceipts().get(0);

        // Check the block was imported, the contract has the Avm prefix, and deployment succeeded.
        assertThat(connectResult.getLeft()).isEqualTo(ImportResult.IMPORTED_BEST);
        assertThat(receipt.getTransactionOutput()[0]).isEqualTo(AddressSpecs.A0_IDENTIFIER);
        assertThat(receipt.isSuccessful()).isTrue();

        // verify that the output is indeed the contract address
        AionAddress contractAddress = TxUtil.calculateContractAddress(transaction);
        assertThat(contractAddress.toByteArray()).isEqualTo(receipt.getTransactionOutput());
    }

    private byte[] getJarBytes(AvmVersion version) {
        IAvmResourceFactory factory = (version == AvmVersion.VERSION_1) ? resourceProvider.factoryForVersion1 : resourceProvider.factoryForVersion2;
        return factory.newContractFactory().getDeploymentBytes(AvmContract.HELLO_WORLD);
    }
}
