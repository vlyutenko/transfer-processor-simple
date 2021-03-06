package com.transfer.core;

import com.transfer.core.event.AccountCreateEvent;
import com.transfer.core.event.AccountInfoEvent;
import com.transfer.core.event.AccountTransferEvent;

public interface AccountEventProcessor extends AutoCloseable{

    void start();

    String process(AccountCreateEvent accountCreateEvent);

    String process(AccountInfoEvent accountInfoEvent);

    String process(AccountTransferEvent accountTransferEvent);
}
