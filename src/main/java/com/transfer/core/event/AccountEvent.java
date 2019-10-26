package com.transfer.core.event;

import com.transfer.core.AccountEventProcessor;

import java.util.function.Consumer;

public abstract class AccountEvent {
    private final Consumer<String> resultConsumer;
    private final Consumer<Throwable> errorConsumer;

    AccountEvent(Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer) {
        this.resultConsumer = resultConsumer;
        this.errorConsumer = errorConsumer;
    }

    public Consumer<String> getResultConsumer() {
        return resultConsumer;
    }

    public Consumer<Throwable> getErrorConsumer() {
        return errorConsumer;
    }

    public abstract String processEvent(AccountEventProcessor visitor);

}
