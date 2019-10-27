package com.transfer.core;

import com.transfer.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class AccountEventPublisherImpl implements AccountEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountEventPublisherImpl.class);

    private final BlockingQueue<AccountEvent> eventBus;

    public AccountEventPublisherImpl(BlockingQueue<AccountEvent> eventBus) {
        this.eventBus = eventBus;
    }

    /*
     * Thread-safe could be called from multiple threads
     */
    @Override
    public boolean publishEvent(AccountEvent accountEvent) {
        try {
            return eventBus.offer(accountEvent, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread was interrupted");
            return false;
        }
    }
}
