package org.nuist.minibase.scanner;

import org.nuist.minibase.KeyValue;

import java.io.IOException;
import java.util.Iterator;
import java.util.SortedMap;

public class IteratorWrapper implements SeekIter<KeyValue> {

    private SortedMap<KeyValue, KeyValue> sortedMap;
    private Iterator<KeyValue> it;

    public IteratorWrapper(SortedMap<KeyValue, KeyValue> sortedMap) {
        this.sortedMap = sortedMap;
        this.it = sortedMap.values().iterator();
    }

    @Override
    public boolean hasNext() throws IOException {
        return it != null && it.hasNext();
    }

    @Override
    public KeyValue next() throws IOException {
        return it.next();
    }

    @Override
    public void seekTo(KeyValue kv) throws IOException {
        it = sortedMap.tailMap(kv).values().iterator();
    }
}