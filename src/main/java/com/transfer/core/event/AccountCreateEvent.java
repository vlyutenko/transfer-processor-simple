package com.transfer.core.event;


import com.transfer.core.AccountEventProcessor;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class AccountCreateEvent extends AccountEvent {

    private final BigDecimal amount;

    public AccountCreateEvent(Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer, BigDecimal amount) {
        super(resultConsumer, errorConsumer);
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String processEvent(AccountEventProcessor visitor) {
        return visitor.process(this);
    }
}
