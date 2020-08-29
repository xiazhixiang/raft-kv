package org.nuist.rpc.test.utils;

import org.nuist.rpc.common.utils.SystemClock;

public class SystemClockTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(SystemClock.millisClock().now());
        Thread.sleep(10000);
    }
}
