package org.nuist.raft;

import org.nuist.raft.entity.LogEntry;

/**
 *
 * @see LogEntry
 *
 */
public interface LogModule {

    void write(LogEntry logEntry) throws Exception;

    LogEntry read(Long index);

    void removeOnStartIndex(Long startIndex) throws Exception;

    LogEntry getLast();

    Long getLastIndex();
}
