package org.nuist.minibase.scanner;

import java.io.IOException;

public interface Iter<KeyValue> {
    boolean hasNext() throws IOException;

    KeyValue next() throws IOException;
}