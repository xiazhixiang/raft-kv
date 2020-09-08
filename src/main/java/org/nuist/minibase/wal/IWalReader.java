package org.nuist.minibase.wal;

import org.nuist.minibase.KeyValue;

import java.io.IOException;
import java.util.Iterator;

/**
 */
public interface IWalReader {

    Iterator<KeyValue> read() throws IOException;

    void close() throws IOException;
}
