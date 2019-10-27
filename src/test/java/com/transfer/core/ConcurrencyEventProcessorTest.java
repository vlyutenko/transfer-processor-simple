package com.transfer.core;

import com.google.gson.Gson;
import com.transfer.core.event.AccountCreateEvent;
import com.transfer.core.event.AccountEvent;
import com.transfer.core.event.AccountInfoEvent;
import com.transfer.core.event.AccountTransferEvent;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyEventProcessorTest {

    private final BlockingQueue<AccountEvent> eventBus = new ArrayBlockingQueue<>(1024);
    private final AccountStorage<AccountInfo, UUID> accountStorage = new AccountStorageImpl();
    private final AccountEventPublisherImpl accountEventPublisher = new AccountEventPublisherImpl(eventBus);
    private final AccountEventProcessor accountEventProcessor = new AccountEventProcessorImpl(accountStorage, eventBus);
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Gson gson = new Gson();

    @BeforeClass
    public void init() {
        accountEventProcessor.start();
    }

    @AfterClass
    public void close() throws Exception{
        accountEventProcessor.close();
    }

    @Test
    public void shouldCorrectServeConcurrentRequests() throws Exception {
        //Creating 2 accounts and saving it UUIDs
        BigDecimal amountFirst = new BigDecimal(200000);
        CompletableFuture<String> futureFirst = new CompletableFuture<>();
        AccountCreateEvent accountCreateEventFirst = new AccountCreateEvent(futureFirst::complete, futureFirst::completeExceptionally, amountFirst);
        accountEventPublisher.publishEvent(accountCreateEventFirst);
        UUID uuidFirst = UUID.fromString((String) gson.fromJson(futureFirst.get(), Map.class).get("uuid"));

        BigDecimal amountSecond = new BigDecimal(300000);
        CompletableFuture<String> futureSecond = new CompletableFuture<>();
        AccountCreateEvent accountCreateEventSecond = new AccountCreateEvent(futureSecond::complete, futureSecond::completeExceptionally, amountSecond);
        accountEventPublisher.publishEvent(accountCreateEventSecond);
        UUID uuidSecond = UUID.fromString((String) gson.fromJson(futureSecond.get(), Map.class).get("uuid"));


        AtomicInteger totalNumberOfRequest = new AtomicInteger(10000);

        //launch concurrently 10 threads and perform concurrent transfers from first to second account and back
        //number of forward and back transfers should be the same, but they will be performed from different threads
        //after all, initial amounts of accounts should be the same
        int processorNum = Runtime.getRuntime().availableProcessors();
        CountDownLatch latch = new CountDownLatch(processorNum);

        for (int i = 0; i < processorNum; i++) {
            executorService.submit(() -> {
                int currentRequest;
                while ((currentRequest = totalNumberOfRequest.decrementAndGet()) >= 0) {
                    //forward transfer
                    UUID fromAccount = uuidFirst;
                    UUID toAccount = uuidSecond;
                    if (currentRequest % 2 == 0) {
                        //every second event will be backward transfer
                        fromAccount = uuidSecond;
                        toAccount = uuidFirst;
                    }

                    CompletableFuture<String> future = new CompletableFuture<>();
                    AccountTransferEvent accountTransferEvent = new AccountTransferEvent(future::complete, future::completeExceptionally, fromAccount, toAccount, new BigDecimal(1));
                    accountEventPublisher.publishEvent(accountTransferEvent);
                    try {
                        future.get();
                    } catch (Exception ignored) {
                    }
                }
                latch.countDown();

            });
        }

        latch.await();
        //amount should stay the same

        futureFirst = new CompletableFuture<>();
        AccountInfoEvent accountInfoEventFirst = new AccountInfoEvent(futureFirst::complete, futureFirst::completeExceptionally, uuidFirst);
        accountEventPublisher.publishEvent(accountInfoEventFirst);
        assertThat(amountFirst).isEqualTo(new BigDecimal((Double) gson.fromJson(futureFirst.get(), Map.class).get("amount")));

        futureSecond = new CompletableFuture<>();
        AccountInfoEvent accountInfoEventSecond = new AccountInfoEvent(futureSecond::complete, futureSecond::completeExceptionally, uuidSecond);
        accountEventPublisher.publishEvent(accountInfoEventSecond);
        assertThat(amountSecond).isEqualTo(new BigDecimal((Double) gson.fromJson(futureSecond.get(), Map.class).get("amount")));
    }
}
