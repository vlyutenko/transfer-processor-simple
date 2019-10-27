package com.transfer.core;


public interface AccountStorage<T, R> {

    void put(T accountInfo);

    T getByUuid(R uuid);
}
