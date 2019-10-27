package com.transfer;

import com.transfer.core.*;
import com.transfer.core.event.AccountEvent;
import com.transfer.transport.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TransferApplication implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferApplication.class);

    private final HttpServer httpServer;
    private final AccountEventProcessor accountEventProcessor;

    TransferApplication() {
        BlockingQueue<AccountEvent> eventBus = new ArrayBlockingQueue<>(1024);
        AccountStorage<AccountInfo, UUID> accountStorage = new AccountStorageImpl();
        AccountEventPublisher accountEventPublisher = new AccountEventPublisherImpl(eventBus);

        this.accountEventProcessor = new AccountEventProcessorImpl(accountStorage, eventBus);
        this.httpServer = new HttpServer(accountEventPublisher);
    }

    public void start() {
        LOGGER.info("About to start exchange application");
        accountEventProcessor.start();
        httpServer.start();
    }

    @Override
    public void close() throws Exception{
        LOGGER.info("About to stop exchange application");
        httpServer.close();
        accountEventProcessor.close();
    }
}
