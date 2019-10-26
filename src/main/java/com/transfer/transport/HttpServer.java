package com.transfer.transport;

import com.transfer.core.AccountEventPublisher;
import com.transfer.core.event.AccountCreateEvent;
import com.transfer.core.event.AccountEvent;
import com.transfer.core.event.AccountInfoEvent;
import com.transfer.core.event.AccountTransferEvent;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;


public class HttpServer implements AutoCloseable{

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    private static final String ACCOUNT_CREATE_REQUEST = "/account/create";
    private static final String ACCOUNT_INFO_REQUEST = "/account/info";
    private static final String ACCOUNT_TRANSFER_REQUEST = "/account/transfer";

    private static final String ACCOUNT_REQUEST_PARAMETER = "account";
    private static final String ACCOUNT_FROM_REQUEST_PARAMETER = "fromAccount";
    private static final String ACCOUNT_TO_REQUEST_PARAMETER = "toAccount";
    private static final String AMOUNT_REQUEST_PARAMETER = "amount";

    private final AccountEventPublisher accountEventPublisher;
    private final Javalin javalin;

    public HttpServer(AccountEventPublisher accountEventPublisher) {
        this.accountEventPublisher = accountEventPublisher;
        this.javalin = Javalin.create();
    }

    public void start() {
        javalin.start(80);

        javalin.get(ACCOUNT_INFO_REQUEST, context -> processEvent(context, context.queryParamMap(), this::newInfoEvent));
        javalin.post(ACCOUNT_CREATE_REQUEST, context -> processEvent(context, context.bodyAsClass(Map.class), this::newCreateEvent));
        javalin.post(ACCOUNT_TRANSFER_REQUEST, context -> processEvent(context, context.bodyAsClass(Map.class), this::newTransferEvent));

        javalin.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType("text/plain");
            ctx.result("Problems during request processing: " + e.toString());
        });
    }

    private void processEvent(Context context, Map parameters, BiFunction<Map, CompletableFuture<String>, AccountEvent> biFunction) {
        try {
            CompletableFuture<String> completableFuture = new CompletableFuture<>();
            AccountEvent accountEvent = biFunction.apply(parameters, completableFuture);
            context.contentType("application/json");
            context.result(completableFuture);
            if (!accountEventPublisher.publishEvent(accountEvent)) {
                throw new IllegalStateException("Not enough resource capacity to process request");
            }
        } catch (Exception ex) {
            LOGGER.error("Problems during request parsing", ex);
            context.status(500);
            context.contentType("text/plain");
            context.result("Problems during request parsing: " + ex.toString());
        }
    }


    private AccountEvent newTransferEvent(Map<String, String> parameters, CompletableFuture<String> future) {
        return new AccountTransferEvent(
                future::complete,
                future::completeExceptionally,
                UUID.fromString(parameters.get(ACCOUNT_FROM_REQUEST_PARAMETER)),
                UUID.fromString(parameters.get(ACCOUNT_TO_REQUEST_PARAMETER)),
                new BigDecimal(parameters.get(AMOUNT_REQUEST_PARAMETER)));
    }

    private AccountEvent newCreateEvent(Map<String, String> parameters, CompletableFuture<String> future) {
        return new AccountCreateEvent(
                future::complete,
                future::completeExceptionally,
                new BigDecimal(parameters.get(AMOUNT_REQUEST_PARAMETER))
        );
    }

    private AccountEvent newInfoEvent(Map<String, List<String>> parameters, CompletableFuture<String> future) {
        return new AccountInfoEvent(
                future::complete,
                future::completeExceptionally,
                UUID.fromString(parameters.get(ACCOUNT_REQUEST_PARAMETER).get(0))
        );
    }

    @Override
    public void close() {
        javalin.stop();
    }
}
