package org.nuist.minibase.scanner;

import org.nuist.minibase.KeyValue;

import java.io.IOException;
import java.util.List;
import java.util.PriorityQueue;

public class MultiIter implements SeekIter<KeyValue> {

    private class IterNode {
        KeyValue kv;
        SeekIter<KeyValue> iter;

        public IterNode(KeyValue kv, SeekIter<KeyValue> it) {
            this.kv = kv;
            this.iter = it;
        }
    }

    private SeekIter<KeyValue> iters[];
    private PriorityQueue<IterNode> queue;

    public MultiIter(SeekIter<KeyValue> iters[]) throws IOException {
        assert iters != null;
        this.iters = iters; // Used for seekTo
        this.queue = new PriorityQueue<>(((o1, o2) -> o1.kv.compareTo(o2.kv)));
        for (int i = 0; i < iters.length; i++) {
            if (iters[i] != null && iters[i].hasNext()) {
                queue.add(new IterNode(iters[i].next(), iters[i]));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public MultiIter(List<SeekIter<KeyValue>> iters) throws IOException {
        this(iters.toArray(new SeekIter[0]));
    }

    @Override
    public boolean hasNext() throws IOException {
        return queue.size() > 0;
    }

    @Override
    public KeyValue next() throws IOException {
        while (!queue.isEmpty()) {
            IterNode first = queue.poll();
            if (first.kv != null && first.iter != null) {
                if (first.iter.hasNext()) {
                    queue.add(new IterNode(first.iter.next(), first.iter));
                }
                return first.kv;
            }
        }
        return null;
    }

    @Override
    public void seekTo(KeyValue kv) throws IOException {
        queue.clear();
        for (SeekIter<KeyValue> it : iters) {
            it.seekTo(kv);
            if (it.hasNext()) {
                // Only the iterator which has some elements should be enqueued.
                queue.add(new IterNode(it.next(), it));
            }
        }
    }
}
