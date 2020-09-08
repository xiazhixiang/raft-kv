package org.nuist.minibase.wal;



import org.nuist.minibase.KeyValue;

import java.io.IOException;

/**
 * IWalWriter
 */
public interface IWalWriter {

    void write(KeyValue kv) throws Exception;

    void close() throws Exception;

    void delete() throws Exception;

    String getWalFileName();
}
