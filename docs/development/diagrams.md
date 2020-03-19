# Diagrams

## EVM class diagram

This class diagram contains the most important and involved classes in the EVM flow.

![EVM Class diagram][EVM Class diagram]

> The method `EVM#runToHalt(MessageFrame, OperationTracer)` allows the execution of a MessageFrame until it halts.

Take particular attention to the `MessageFrame` class, this class holds the behavior and the interaction between two accounts. The states of `MessageFrame` are:

![MessageFrame state machine][MessageFrame state machine]

## Sequence diagrams

This section contains the relevant sequence diagrams for checking an account balance, sending a call (which does not generate a transaction), and making a transaction on a smart contract.

### eth_getBalance

[JSON-RPC call](https://documenter.getpostman.com/view/4117254/ethereum-json-rpc/RVu7CT5J?version=latest#2166dfcf-f1a5-8ba7-6099-4808b41389a3)

![eth_getBalance][eth_getBalance]


### eth_call

[JSON-RPC call](https://documenter.getpostman.com/view/4117254/ethereum-json-rpc/RVu7CT5J?version=latest#ac5be134-204f-3aa2-d527-e973cc2df968)

![eth_call][eth_call]

In this case the class `TransactionSimulator` it is used to execute read operations on the blockchain or to estimate the transaction gas cost. The processing won't affect the world state.


### eth_sendRawTransaction

A transaction call is splitted in two different flows: the eth_sendRawTransaction and the mining block process.

#### Send raw transaction request

[JSON-RPC call](https://documenter.getpostman.com/view/4117254/ethereum-json-rpc/RVu7CT5J?version=latest#150dbf79-b762-e585-c345-fc666da5e354)

![eth_sendRawTransaction][eth_sendRawTransaction]

#### Mine block

![Mine Block][mine-block]

The method `MainnetTransactionProcessor#processTransaction(Blockchain,WorldUpdater,ProcessableBlockHeader,Transaction,Address,OperationTracer,BlockHashLookup,isPersistingState:Boolean,TransactionValidationParams): Result` create a `MessageFrame` with the necessary information to process the current transacction.




[EVM Class diagram]: ../images/evm-class_diagram.svg
[MessageFrame state machine]: ../images/MessageFrame-state_machine.svg
[eth_getBalance]: ../images/eth_getBalance-sequence.svg
[eth_call]: ../images/eth_call-sequence.svg
[eth_sendRawTransaction]: ../images/eth_sendRawTransaction-sequence.svg
[mine-block]: ../images/mineBlock-sequence.svg
