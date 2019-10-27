package com.transfer.core;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.transfer.core.event.AccountCreateEvent;
import com.transfer.core.event.AccountEvent;
import com.transfer.core.event.AccountInfoEvent;
import com.transfer.core.event.AccountTransferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AccountEventProcessorImpl implements AccountEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountEventProcessorImpl.class);

    private final AccountStorage<AccountInfo, UUID> accountStorage;
    private final AtomicBoolean running;
    private final ExecutorService executorService;
    private final Gson gson;
    private final BlockingQueue<AccountEvent> eventBus;

    public AccountEventProcessorImpl(AccountStorage<AccountInfo, UUID> accountStorage, BlockingQueue<AccountEvent> eventBus) {
        this.accountStorage = accountStorage;
        this.eventBus = eventBus;
        this.running = new AtomicBoolean(true);
        //only one processor thread to omit synchronisation on events, because all events are computationally not heavy, application will be bounded by IO
        //in case of heavy computation model should be changed
        this.executorService = Executors.newFixedThreadPool(1);
        this.gson = new Gson();
    }

    /*
     * Start processor thread which check available events in event queue and process it
     */
    public void start() {
        executorService.submit(() -> {
            while (running.get()) {
                AccountEvent accountEvent;
                if ((accountEvent = eventBus.poll()) != null) {
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


    public String process(AccountTransferEvent accountTransferEvent) {
        AccountInfo fromAccount = accountStorage.getByUuid(accountTransferEvent.getAccountFrom());
        if (fromAccount == null) {
            throw new IllegalArgumentException("from account not present in storage");
        }

        AccountInfo toAccount = accountStorage.getByUuid(accountTransferEvent.getAccountTo());

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
        AccountInfo accountInfo = accountStorage.getByUuid(accountInfoEvent.getAccount());
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
        accountStorage.put(accountInfo);

        LOGGER.info("{} account created with amount {}", uuid, accountCreateEvent.getAmount());
        return gson.toJson(accountInfo);
    }

    @Override
    public void close() {
        running.set(false);
        executorService.shutdown();
    }
}
