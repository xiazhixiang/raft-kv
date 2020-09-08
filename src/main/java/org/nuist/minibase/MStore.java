package org.nuist.minibase;

import org.nuist.minibase.compactor.Compactor;
import org.nuist.minibase.compactor.DefaultCompactor;
import org.nuist.minibase.conf.Configuration;
import org.nuist.minibase.flush.DefaultFlusher;
import org.nuist.minibase.mem.MemStore;
import org.nuist.minibase.scanner.Iter;
import org.nuist.minibase.scanner.MultiIter;
import org.nuist.minibase.scanner.ScanIter;
import org.nuist.minibase.scanner.SeekIter;
import org.nuist.minibase.store.DiskStore;
import org.nuist.minibase.utils.Bytes;
import org.nuist.minibase.wal.FileWalWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class MStore implements MiniBase{

    private ExecutorService pool;
    private MemStore memStore;
    private DiskStore diskStore;
   private Compactor compactor;
    private AtomicLong sequenceId;

    private Configuration conf;


    public static MStore create(Configuration conf) {
        return new MStore(conf);
    }

    public MiniBase open() throws Exception {
        assert conf != null;

        // initialize the thread pool;
        this.pool = Executors.newFixedThreadPool(conf.getMaxThreadPoolSize());

        // initialize the disk store.
        this.diskStore = new DiskStore(conf.getDataDir(), conf.getMaxDiskFiles());
        this.diskStore.open();
        // TODO initialize the max sequence id here.
        this.sequenceId = new AtomicLong(0);

        // initialize the memstore.
        this.memStore = new MemStore(conf, new DefaultFlusher(diskStore), pool);
        this.compactor = new DefaultCompactor(diskStore);
        this.compactor.start();
        return this;
    }

    private MStore(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public void put(byte[] key, byte[] value) throws Exception {

        this.memStore.add(KeyValue.createPut(key, value, sequenceId.incrementAndGet()));
    }

    @Override
    public KeyValue get(byte[] key) throws IOException {
        if(key == null)
            return null;
        KeyValue result = null;
        Iter<KeyValue> it = scan(key, Bytes.EMPTY_BYTES);
        if (it.hasNext()) {
            KeyValue kv = it.next();
            if (Bytes.compare(kv.getKey(), key) == 0) {
                result = kv;
            }
        }
        return result;
    }

    @Override
    public void delete(byte[] key) throws IOException {

    }

    @Override
    public Iter<KeyValue> scan(byte[] start, byte[] stop) throws IOException {
        List<SeekIter<KeyValue>> iterList = new ArrayList<>();
        //kvmap和snapshot迭代
        iterList.add(memStore.createIterator());
        //构建所有文件的scanner
        iterList.add(diskStore.createIterator());
        MultiIter it = new MultiIter(iterList);

        // with start being EMPTY_BYTES means minus infinity, will skip to seek.
        if (Bytes.compare(start, Bytes.EMPTY_BYTES) != 0) {
            it.seekTo(KeyValue.createDelete(start, sequenceId.get()));
        }

        KeyValue stopKV = null;
        if (Bytes.compare(stop, Bytes.EMPTY_BYTES) != 0) {
            // the smallest kv in all KeyValue with the same key.
            stopKV = KeyValue.createDelete(stop, Long.MAX_VALUE);
        }
        return new ScanIter(stopKV, it);
    }

    @Override
    public void close() throws IOException {

    }
}
