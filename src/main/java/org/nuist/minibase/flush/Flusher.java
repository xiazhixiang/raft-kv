package org.nuist.minibase.flush;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.scanner.Iter;

import java.io.IOException;

public interface Flusher {
    void flush(Iter<KeyValue> it) throws IOException;
}
