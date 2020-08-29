package org.nuist.minibase.scanner;

import org.nuist.minibase.KeyValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

public class MemStoreIter implements SeekIter<KeyValue> {

    private MultiIter it;

    public MemStoreIter(NavigableMap<KeyValue, KeyValue> kvMap,
                        NavigableMap<KeyValue, KeyValue> snapshot) throws IOException {
        List<IteratorWrapper> inputs = new ArrayList<>();
        if (kvMap != null && kvMap.size() > 0) {
            inputs.add(new IteratorWrapper(kvMap));
        }
        if (snapshot != null && snapshot.size() > 0) {
            inputs.add(new IteratorWrapper(snapshot));
        }
        it = new MultiIter(inputs.toArray(new IteratorWrapper[0]));
    }

    @Override
    public boolean hasNext() throws IOException {
        return it.hasNext();
    }

    @Override
    public KeyValue next() throws IOException {
        return it.next();
    }

    @Override
    public void seekTo(KeyValue kv) throws IOException {
        it.seekTo(kv);
    }
}
