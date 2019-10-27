package com.transfer.transport

import com.transfer.core.AccountEventProcessorImpl
import com.transfer.core.AccountEventPublisherImpl
import com.transfer.core.AccountStorageImpl
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue

import static groovyx.net.http.ContentType.JSON

class HttpServerSpec extends Specification {

    @Shared
            eventBus = new ArrayBlockingQueue<>(1024)
    @Shared
            accountStorage = new AccountStorageImpl();
    @Shared
            accountEventPublisher = new AccountEventPublisherImpl(eventBus)
    @Shared
            accountEventProcessor = new AccountEventProcessorImpl(accountStorage, eventBus);
    @Shared
            httpServer = new HttpServer(accountEventPublisher)
    @Shared
            client = new RESTClient('http://localhost:80/')


    def setupSpec() {
        accountEventProcessor.start()
        httpServer.start()
    }

    def cleanupSpec() {
        httpServer.close()
        accountEventProcessor.close()
    }

    def "should create account"() {
        given:
        def amount = 2000.5

        when:
        def response = client.post(path: '/account/create', body: [amount: Double.toString(amount)], requestContentType: JSON)

        then:
        assert response.status == 200: 'response status should be 200 if amount is positive number'
        assert response.data.amount == amount: 'amount should be equal to initial request parameter amount'
    }

    def "should not create account with negative amount"() {
        given:
        def amount = -2000

        when:
        client.post(path: '/account/create', body: [amount: Integer.toString(amount)], requestContentType: JSON)

        then: 'server returns 500 code (server error)'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of serve side exception'
    }

    def "should not create account with wrong type amount"() {
        given:
        def amount = 'XXX'

        when:
        client.post(path: '/account/create', body: [amount: amount], requestContentType: JSON)

        then: 'server returns 500 code (server error)'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of serve side exception'
    }

    def "should not create account with missed amount parameter"() {
        when:
        client.post(path: '/account/create')

        then: 'server returns 400 bad request'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 400: 'response status should be 400 because of bad request'
    }

    def "should create account and get info"() {
        given:
        def amount = 2000

        when: 'create account and get UUID from response'
        def response = client.post(path: '/account/create', body: [amount: Integer.toString(amount)], requestContentType: JSON)
        def uuid = response.data.uuid

        and: 'get info with UUID from previous step'
        response = client.get(path: '/account/info', query: [account: uuid])

        then:
        assert response.status == 200: 'response status should be 200 if uuid is present'
        assert response.data.uuid == uuid: 'uuid should be the same returned by create request'
        assert response.data.amount == amount: 'amount should be equal to initial request parameter amount'
    }

    def "should not get info with wrong account type"() {
        given:
        def uuid = 'XXX'

        when:
        response = client.get(path: '/account/info', query: [account: uuid])

        then: 'server returns 500'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of parsing error'
    }

    def "should not get info with not present account"() {
        given:
        def uuid = '1473b088-f333-11e9-a713-2a2ae2dbcce4'

        when:
        response = client.get(path: '/account/info', query: [account: uuid])

        then: 'server returns 500'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because account not present in storage'
    }

    def "should not get info in missed account param"() {
        when:
        response = client.get(path: '/account/info')

        then: 'server returns 500'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of missed param during parsing'
    }

    def "should correct transfer between accounts"() {
        given:
        def amountFrom = 2000
        def amountTo = 3000
        def transferAmount = 20

        when: 'create account FROM and get UUID from response'
        def response = client.post(path: '/account/create', body: [amount: Integer.toString(amountFrom)], requestContentType: JSON)
        def uuidFrom = response.data.uuid

        and: 'create account TO and get UUID from response'
        response = client.post(path: '/account/create', body: [amount: Integer.toString(amountTo)], requestContentType: JSON)
        def uuidTo = response.data.uuid

        and: 'perform transfer between just created accounts'
        response = client.post(path: '/account/transfer',
                body: [amount     : Integer.toString(transferAmount),
                       fromAccount: uuidFrom,
                       toAccount  : uuidTo
                ], requestContentType: JSON)

        then:
        assert response.status == 200: 'response status should be 200 if all params ok'
        assert response.data[0].uuid == uuidFrom: 'should be uuid FROM'
        assert response.data[0].amount == amountFrom - transferAmount: 'amount should be decreased by transfer amount'
        assert response.data[1].uuid == uuidTo: 'should be uuid TO'
        assert response.data[1].amount == amountTo + transferAmount: 'amount should be increased by transfer amount'
    }


    def "should not transfer between accounts if not enough money on account"() {
        given:
        def amountFrom = 2000
        def amountTo = 3000
        def transferAmount = 20000

        when: 'create account FROM and get UUID from response'
        def response = client.post(path: '/account/create', body: [amount: Integer.toString(amountFrom)], requestContentType: JSON)
        def uuidFrom = response.data.uuid

        and: 'create account TO and get UUID from response'
        response = client.post(path: '/account/create', body: [amount: Integer.toString(amountTo)], requestContentType: JSON)
        def uuidTo = response.data.uuid

        and: 'perform transfer between just created accounts'
        client.post(path: '/account/transfer',
                body: [amount     : Integer.toString(transferAmount),
                       fromAccount: uuidFrom,
                       toAccount  : uuidTo
                ], requestContentType: JSON)

        then: 'server returns 500'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of not enough money'
    }


    def "should not transfer between accounts if negative amount"() {
        given:
        def amountFrom = 2000
        def amountTo = 3000
        def transferAmount = -20000

        when: 'create account FROM and get UUID from response'
        def response = client.post(path: '/account/create', body: [amount: Integer.toString(amountFrom)], requestContentType: JSON)
        def uuidFrom = response.data.uuid

        and: 'create account TO and get UUID from response'
        response = client.post(path: '/account/create', body: [amount: Integer.toString(amountTo)], requestContentType: JSON)
        def uuidTo = response.data.uuid

        and: 'perform transfer between just created accounts'
        client.post(path: '/account/transfer',
                body: [amount     : Integer.toString(transferAmount),
                       fromAccount: uuidFrom,
                       toAccount  : uuidTo
                ], requestContentType: JSON)

        then: 'server returns 500'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of not enough money'
    }

    def "should not transfer on non existing account"() {
        given:
        def amountFrom = 2000
        def uuidTo = '1473b088-f333-11e9-a713-2a2ae2dbcce4'
        def transferAmount = -20000

        when: 'create account FROM and get UUID from response'
        def response = client.post(path: '/account/create', body: [amount: Integer.toString(amountFrom)], requestContentType: JSON)
        def uuidFrom = response.data.uuid

        and: 'perform transfer between created and non existing accounts'
        client.post(path: '/account/transfer',
                body: [amount     : Integer.toString(transferAmount),
                       fromAccount: uuidFrom,
                       toAccount  : uuidTo
                ], requestContentType: JSON)

        then: 'server returns 500'
        HttpResponseException e = thrown(HttpResponseException)
        assert e.response.status == 500: 'response status should be 500 because of non existing account'
    }
}
