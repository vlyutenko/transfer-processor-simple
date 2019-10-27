package com.transfer.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountStorageImpl implements AccountStorage<AccountInfo, UUID> {

    //Non thread safe map is used as a storage, because access to this map is performed always from the same thread
    private final Map<UUID, AccountInfo> storage = new HashMap<>();

    @Override
    public void put(AccountInfo accountInfo) {
        storage.put(accountInfo.getUuid(), accountInfo);
    }

    @Override
    public AccountInfo getByUuid(UUID uuid) {
        return storage.get(uuid);
    }
}
