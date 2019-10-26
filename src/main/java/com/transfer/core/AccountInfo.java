package com.transfer.core;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountInfo {

    private final UUID uuid;
    private BigDecimal amount;

    public AccountInfo(UUID uuid, BigDecimal amount) {
        this.uuid = uuid;
        this.amount = amount;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
