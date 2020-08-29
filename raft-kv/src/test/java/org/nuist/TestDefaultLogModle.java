package org.nuist;

import org.nuist.raft.entity.LogEntry;
import org.nuist.raft.impl.DefaultLogModule;

public class TestDefaultLogModle {

    public static void main(String[] args) throws Exception {
        new DefaultLogModule().write(new LogEntry());

    }
}
