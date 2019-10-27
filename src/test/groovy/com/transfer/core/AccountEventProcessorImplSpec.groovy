package com.transfer.core

import com.transfer.core.event.AccountCreateEvent
import com.transfer.core.event.AccountInfoEvent
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue

class AccountEventProcessorImplSpec extends Specification {

    AccountStorage accountStorage = Mock()
    AccountEventProcessor accountEventProcessor = new AccountEventProcessorImpl(accountStorage, new ArrayBlockingQueue<>(10))

    def "should call put storage on create event"() {
        given:
        def createEvent = new AccountCreateEvent({ _ -> }, { _ -> }, new BigDecimal(1))
        when:
        accountEventProcessor.process(createEvent)
        then:
        1 * accountStorage.put(_)
    }

    def "should call getByUuid storage on info event"() {
        given:
        def uuid = UUID.randomUUID()
        def infoEvent = new AccountInfoEvent({ _ -> }, { _ -> }, uuid)
        when:
        accountEventProcessor.process(infoEvent)
        then:
        thrown(IllegalArgumentException)
        1 * accountStorage.getByUuid(uuid)
    }

    //e.t.c

}
