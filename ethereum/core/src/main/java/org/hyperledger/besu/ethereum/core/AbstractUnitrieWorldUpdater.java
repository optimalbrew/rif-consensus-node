package org.hyperledger.besu.ethereum.core;

import co.rsk.db.MutableRepository;

public abstract class AbstractUnitrieWorldUpdater<W extends WorldView, A extends Account> extends AbstractWorldUpdater {

    private MutableRepository repository;

    protected AbstractUnitrieWorldUpdater(final W world, MutableRepository repository) {
        super(world);
        this.repository = repository;
    }

    @Override
    public DefaultEvmAccount createAccount(Address address, long nonce, Wei balance) {
        DefaultEvmAccount account =  super.createAccount(address, nonce, balance);
        repository.updateAccountState(address, account);
        return  account;
    }
}
