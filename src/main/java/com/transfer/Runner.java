package com.transfer;


import org.agrona.concurrent.SigIntBarrier;

public class Runner {

    public static void main(String[] args) {
        try (TransferApplication transferApplication = new TransferApplication()) {
            transferApplication.start();
            new SigIntBarrier().await();
        }
    }
}
