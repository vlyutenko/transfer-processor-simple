package com.transfer;

import com.transfer.core.AccountEventPublisherImpl;
import com.transfer.transport.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferApplication implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferApplication.class);

    private final AccountEventPublisherImpl accountEventProcessorImpl;
    private final HttpServer httpServer;

    TransferApplication() {
        this.accountEventProcessorImpl = new AccountEventPublisherImpl();
        this.httpServer = new HttpServer(accountEventProcessorImpl);
    }

    public void start() {
        LOGGER.info("About to start exchange application");
        accountEventProcessorImpl.start();
        httpServer.start();
    }

    @Override
    public void close() {
        LOGGER.info("About to stop exchange application");
        httpServer.close();
        accountEventProcessorImpl.close();
    }
}
