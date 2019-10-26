package com.transfer.core.event;

import com.transfer.core.AccountEventProcessor;

import java.util.UUID;
import java.util.function.Consumer;

public class AccountInfoEvent extends AccountEvent {
    private final UUID account;

    public AccountInfoEvent(Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer, UUID account) {
        super(resultConsumer, errorConsumer);
        this.account = account;
    }

    public UUID getAccount() {
        return account;
    }


    public String processEvent(AccountEventProcessor visitor) {
        return visitor.process(this);
    }
}
