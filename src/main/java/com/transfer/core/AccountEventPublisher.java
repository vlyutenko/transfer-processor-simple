package com.transfer.core;

import com.transfer.core.event.AccountEvent;

public interface AccountEventPublisher {

    boolean publishEvent(AccountEvent accountEvent);
}
