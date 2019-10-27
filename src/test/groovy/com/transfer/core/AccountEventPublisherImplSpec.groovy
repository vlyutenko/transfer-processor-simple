package com.transfer.core

import com.transfer.core.event.AccountCreateEvent
import com.transfer.core.event.AccountEvent
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class AccountEventPublisherImplSpec extends Specification {

    def capacity = 1
    BlockingQueue<AccountEvent> eventBus = new ArrayBlockingQueue<>(capacity);
    AccountEventPublisher accountEventPublisher = new AccountEventPublisherImpl(eventBus)

    def "publish event should return false when queue is full"() {
        given:
        def createEvent = new AccountCreateEvent({ _ -> }, { _ -> }, new BigDecimal(1))
        when: "add events under capacity"
        (1..capacity).each {
            accountEventPublisher.publishEvent(createEvent)
        }
        then: "should return false when events are out of capacity"
        assert !accountEventPublisher.publishEvent(createEvent)

    }
}
