package com.transfer.core;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.transfer.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AccountEventPublisherImpl implements AutoCloseable, AccountEventProcessor, AccountEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountEventPublisherImpl.class);

    //Non thread safe map is used as a storage, because access to this map is performed always from the same thread
    private final Map<UUID, AccountInfo> accountStorage = new HashMap<>();
    private final ArrayBlockingQueue<AccountEvent> eventQueue = new ArrayBlockingQueue<>(1024);
    private final AtomicBoolean running = new AtomicBoolean(true);
    //only one processor thread to omit synchronisation on events, because all events are computationally not heavy, application will be bounded by IO
    //in case of heavy computation model should be changed
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final Gson gson = new Gson();

    /*
     * Start processor thread which check available events in event queue and process it
     */
    public void start() {
        executorService.submit(() -> {
            while (running.get()) {
                AccountEvent accountEvent;
                if ((accountEvent = eventQueue.poll()) != null) {
                    handleEvent(accountEvent);
                }
            }
        });
    }

    /*
     * Thread safe, because is executed in the same thread all the time (only from executor)
     */
    private void handleEvent(AccountEvent event) {
        try {
            String response = event.processEvent(this);
            event.getResultConsumer().accept(response);
        } catch (Exception ex) {
            LOGGER.error("Problems during event processing", ex);
            event.getErrorConsumer().accept(ex);
        }
    }

    /*
     * Thread-safe could be called from multiple threads
     */
    @Override
    public boolean publishEvent(AccountEvent accountEvent) {
        try {
            return eventQueue.offer(accountEvent, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread was interrupted");
            return false;
        }
    }

    public String process(AccountTransferEvent accountTransferEvent) {
        AccountInfo fromAccount = accountStorage.get(accountTransferEvent.getAccountFrom());
        if (fromAccount == null) {
            throw new IllegalArgumentException("from account not present in storage");
        }

        AccountInfo toAccount = accountStorage.get(accountTransferEvent.getAccountTo());

        if (toAccount == null) {
            throw new IllegalArgumentException("to account not present in storage");
        }

        BigDecimal amount = accountTransferEvent.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Should not be less then 0");
        }

        if (fromAccount.getAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Not enough money for transfer");
        }

        fromAccount.setAmount(fromAccount.getAmount().subtract(amount));
        toAccount.setAmount(toAccount.getAmount().add(amount));

        LOGGER.info("Transfer from account {} to account {}, amount {}", fromAccount.getUuid(), toAccount.getUuid(), amount);

        return gson.toJson(ImmutableList.of(fromAccount, toAccount));
    }

    public String process(AccountInfoEvent accountInfoEvent) {
        AccountInfo accountInfo = accountStorage.get(accountInfoEvent.getAccount());
        if (accountInfo == null) {
            throw new IllegalArgumentException("account not present");
        }

        LOGGER.info("{} available amount for account {}", accountInfo.getAmount(), accountInfo.getUuid());
        return gson.toJson(accountInfo);
    }

    public String process(AccountCreateEvent accountCreateEvent) {
        if (accountCreateEvent.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Should not be less then 0");
        }
        UUID uuid = UUID.randomUUID();
        AccountInfo accountInfo = new AccountInfo(uuid, accountCreateEvent.getAmount());
        accountStorage.put(uuid, accountInfo);

        LOGGER.info("{} account created with amount {}", uuid, accountCreateEvent.getAmount());
        return gson.toJson(accountInfo);
    }

    @Override
    public void close() {
        running.set(false);
        executorService.shutdown();
    }
}
