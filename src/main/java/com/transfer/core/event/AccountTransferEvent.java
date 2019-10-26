package com.transfer.core.event;


import com.transfer.core.AccountEventProcessor;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

public class AccountTransferEvent extends AccountEvent {
    private final UUID accountFrom;
    private final UUID accountTo;
    private final BigDecimal amount;

    public AccountTransferEvent(Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer, UUID accountFrom, UUID accountTo, BigDecimal amount) {
        super(resultConsumer, errorConsumer);
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.amount = amount;
    }

    public UUID getAccountFrom() {
        return accountFrom;
    }


    public UUID getAccountTo() {
        return accountTo;
    }


    public BigDecimal getAmount() {
        return amount;
    }


    public String processEvent(AccountEventProcessor visitor) {
        return visitor.process(this);
    }
}
