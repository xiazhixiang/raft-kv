package org.nuist.raft;

/**
 *
 *
 */
public interface LifeCycle {

    void init() throws Throwable;

    void destroy() throws Throwable;
}
