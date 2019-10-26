Some explantions:
As rest provider I used javalin - simple rest framework very similiar to javaspark,
but it supports asynchronious executions.

Main idea is that I get multiple concurrent reqeusts, throuh the rest api and submit all these events to main
applications module.

This module consists of BlockingQueue, where all events are submited, and one processor thread which drain events from queue 
and process them.

As a storage I use simple map (no need thread safe) because access to this map is always performed from the same thread.

Because all events are not computationaly intensive and application will be bounded more by IO, I can use this simple approach 
and omit all the complexy of syncronisation, two-phase locking e.t.c

I also used spoc as test frameworks and have some concurency tests.

Pros:
- simple architecture
- easy to read 
- easy to maintain  
- easy to extend
- easy to test

Cons:
- not so fast
- rest framework is not for high load
- a lot of object allocations
- gc preasure
- blocking queue is no fast enough

I also create very high performance solution which used Netty + Disruptor LMAX, it has almost zero allocations,
it can be found here:
https://github.com/vlyutenko/transfer-processor
