package org.hyperledger.besu.ethereum.blockcreation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.DefaultEvmAccount;
import org.hyperledger.besu.ethereum.core.Gas;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.ProcessableBlockHeader;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.mainnet.AbstractMessageProcessor;
import org.hyperledger.besu.ethereum.mainnet.MainnetTransactionProcessor;
import org.hyperledger.besu.ethereum.mainnet.TransactionProcessor;
import org.hyperledger.besu.ethereum.mainnet.TransactionValidationParams;
import org.hyperledger.besu.ethereum.mainnet.TransactionValidator;
import org.hyperledger.besu.ethereum.mainnet.ValidationResult;
import org.hyperledger.besu.ethereum.vm.BlockHashLookup;
import org.hyperledger.besu.ethereum.vm.Code;
import org.hyperledger.besu.ethereum.vm.MessageFrame;
import org.hyperledger.besu.ethereum.vm.OperationTracer;

import java.util.ArrayDeque;
import java.util.Deque;

public class AionVmTransactionProcessor implements TransactionProcessor {

    private static final Logger LOG = LogManager.getLogger();

    private final AbstractMessageProcessor contractCreationProcessor = null;

    private final AbstractMessageProcessor messageCallProcessor = null;

    private final int createContractAccountVersion = Account.DEFAULT_VERSION;
    private final int maxStackSize = 1024;

    private final boolean clearEmptyAccounts = true;

    @Override
    public Result processTransaction(final Blockchain blockchain, final WorldUpdater worldState, final ProcessableBlockHeader blockHeader, final Transaction transaction, final Address miningBeneficiary,
                                     final OperationTracer operationTracer,
                                     final BlockHashLookup blockHashLookup,
                                     final Boolean isPersistingPrivateState,
                                     final TransactionValidationParams transactionValidationParams) {
        LOG.trace("Starting execution of {}", transaction);

        ValidationResult<TransactionValidator.TransactionInvalidReason> validationResult =  ValidationResult.valid();//transactionValidator.validate(transaction); TODO implement validator
        // Make sure the transaction is intrinsically valid before trying to
        // compare against a sender account (because the transaction may not
        // be signed correctly to extract the sender).
        if (!validationResult.isValid()) {
            LOG.warn("Invalid transaction: {}", validationResult.getErrorMessage());
            return MainnetTransactionProcessor.Result.invalid(validationResult);
        }

        final Address senderAddress = transaction.getSender();
        final DefaultEvmAccount sender = worldState.getOrCreate(senderAddress);
        // validationResult = transactionValidator.validateForSender(transaction, sender, transactionValidationParams); TODO validate sender
        if (!validationResult.isValid()) {
            LOG.warn("Invalid transaction: {}", validationResult.getErrorMessage());
            return MainnetTransactionProcessor.Result.invalid(validationResult);
        }

        final MutableAccount senderMutableAccount = sender.getMutable();
        final long previousNonce = senderMutableAccount.incrementNonce();
        LOG.trace(
                "Incremented sender {} nonce ({} -> {})", senderAddress, previousNonce, sender.getNonce());

        final Wei upfrontGasCost = transaction.getUpfrontGasCost();
        final Wei previousBalance = senderMutableAccount.decrementBalance(upfrontGasCost);
        LOG.trace(
                "Deducted sender {} upfront gas cost {} ({} -> {})",
                senderAddress,
                upfrontGasCost,
                previousBalance,
                sender.getBalance());

        final Gas intrinsicGas = Gas.MAX_VALUE; // gasCalculator.transactionIntrinsicGasCost(transaction); TODO implement gas calculator
        final Gas gasAvailable = Gas.of(transaction.getGasLimit()).minus(intrinsicGas);
        LOG.trace(
                "Gas available for execution {} = {} - {} (limit - intrinsic)",
                gasAvailable,
                transaction.getGasLimit(),
                intrinsicGas);

        final WorldUpdater worldUpdater = worldState.updater();
        final MessageFrame initialFrame;
        final Deque<MessageFrame> messageFrameStack = new ArrayDeque<>();
        if (transaction.isContractCreation()) {
            final Address contractAddress =
                    Address.contractAddress(senderAddress, sender.getNonce() - 1L);

            initialFrame =
                    MessageFrame.builder()
                            .type(MessageFrame.Type.CONTRACT_CREATION)
                            .messageFrameStack(messageFrameStack)
                            .blockchain(blockchain)
                            .worldState(worldUpdater.updater())
                            .initialGas(gasAvailable)
                            .address(contractAddress)
                            .originator(senderAddress)
                            .contract(contractAddress)
                            .contractAccountVersion(createContractAccountVersion)
                            .gasPrice(transaction.getGasPrice())
                            .inputData(Bytes.EMPTY)
                            .sender(senderAddress)
                            .value(transaction.getValue())
                            .apparentValue(transaction.getValue())
                            .code(new Code(transaction.getPayload()))
                            .blockHeader(blockHeader)
                            .depth(0)
                            .completer(c -> {})
                            .miningBeneficiary(miningBeneficiary)
                            .blockHashLookup(blockHashLookup)
                            .isPersistingPrivateState(isPersistingPrivateState)
                            .maxStackSize(maxStackSize)
                            .transactionHash(transaction.getHash())
                            .build();

        } else {
            final Address to = transaction.getTo().get();
            final Account contract = worldState.get(to);

            initialFrame =
                    MessageFrame.builder()
                            .type(MessageFrame.Type.MESSAGE_CALL)
                            .messageFrameStack(messageFrameStack)
                            .blockchain(blockchain)
                            .worldState(worldUpdater.updater())
                            .initialGas(gasAvailable)
                            .address(to)
                            .originator(senderAddress)
                            .contract(to)
                            .contractAccountVersion(
                                    contract != null ? contract.getVersion() : Account.DEFAULT_VERSION)
                            .gasPrice(transaction.getGasPrice())
                            .inputData(transaction.getPayload())
                            .sender(senderAddress)
                            .value(transaction.getValue())
                            .apparentValue(transaction.getValue())
                            .code(new Code(contract != null ? contract.getCode() : Bytes.EMPTY))
                            .blockHeader(blockHeader)
                            .depth(0)
                            .completer(c -> {})
                            .miningBeneficiary(miningBeneficiary)
                            .blockHashLookup(blockHashLookup)
                            .maxStackSize(maxStackSize)
                            .isPersistingPrivateState(isPersistingPrivateState)
                            .transactionHash(transaction.getHash())
                            .build();
        }

        messageFrameStack.addFirst(initialFrame);

        while (!messageFrameStack.isEmpty()) {
            process(messageFrameStack.peekFirst(), operationTracer);
        }

        if (initialFrame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
            worldUpdater.commit();
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Gas used by transaction: {}, by message call/contract creation: {}",
                    () -> Gas.of(transaction.getGasLimit()).minus(initialFrame.getRemainingGas()),
                    () -> gasAvailable.minus(initialFrame.getRemainingGas()));
        }

        // Refund the sender by what we should and pay the miner fee (note that we're doing them one
        // after the other so that if it is the same account somehow, we end up with the right result)
        final Gas selfDestructRefund = Gas.MAX_VALUE;//gasCalculator.getSelfDestructRefundAmount().times(initialFrame.getSelfDestructs().size()); FIXME urgent
        final Gas refundGas = initialFrame.getGasRefund().plus(selfDestructRefund);
        final Gas refunded = refunded(transaction, initialFrame.getRemainingGas(), refundGas);
        final Wei refundedWei = refunded.priceFor(transaction.getGasPrice());
        senderMutableAccount.incrementBalance(refundedWei);

        final MutableAccount coinbase = worldState.getOrCreate(miningBeneficiary).getMutable();
        final Gas coinbaseFee = Gas.of(transaction.getGasLimit()).minus(refunded);
        final Wei coinbaseWei = coinbaseFee.priceFor(transaction.getGasPrice());
        coinbase.incrementBalance(coinbaseWei);

        initialFrame.getSelfDestructs().forEach(worldState::deleteAccount);

        if (clearEmptyAccounts) {
            clearEmptyAccounts(worldState);
        }

        if (initialFrame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
            return MainnetTransactionProcessor.Result.successful(
                    initialFrame.getLogs(),
                    refunded.toLong(),
                    initialFrame.getOutputData(),
                    validationResult);
        } else {
            return MainnetTransactionProcessor.Result.failed(refunded.toLong(), validationResult, initialFrame.getRevertReason());
        }
    }

    private static void clearEmptyAccounts(final WorldUpdater worldState) {
        worldState.getTouchedAccounts().stream()
                .filter(Account::isEmpty)
                .forEach(a -> worldState.deleteAccount(a.getAddress()));
    }

    private void process(final MessageFrame frame, final OperationTracer operationTracer) {
        final AbstractMessageProcessor executor = getMessageProcessor(frame.getType());

        executor.process(frame, operationTracer);
    }

    private AbstractMessageProcessor getMessageProcessor(final MessageFrame.Type type) {
        switch (type) {
            case MESSAGE_CALL:
                return messageCallProcessor;
            case CONTRACT_CREATION:
                return contractCreationProcessor;
            default:
                throw new IllegalStateException("Request for unsupported message processor type " + type);
        }
    }

    // Not modified
    private static Gas refunded(final Transaction transaction, final Gas gasRemaining, final Gas gasRefund) {
        // Integer truncation takes care of the the floor calculation needed after the divide.
        final Gas maxRefundAllowance =
                Gas.of(transaction.getGasLimit()).minus(gasRemaining).dividedBy(2);
        final Gas refundAllowance = maxRefundAllowance.min(gasRefund);
        return gasRemaining.plus(refundAllowance);
    }
}
