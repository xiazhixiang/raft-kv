package org.nuist.raft.current;

import java.util.concurrent.ThreadFactory;

public class NameThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new RaftThread("Raft thread", r);
        t.setDaemon(true);
        t.setPriority(5);
        return t;
    }
}