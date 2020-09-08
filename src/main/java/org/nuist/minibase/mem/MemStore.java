package org.nuist.minibase.mem;


import org.nuist.minibase.KeyValue;
import org.nuist.minibase.conf.Configuration;
import org.nuist.minibase.file.DiskFile;
import org.nuist.minibase.flush.Flusher;
import org.nuist.minibase.scanner.IteratorWrapper;
import org.nuist.minibase.scanner.MemStoreIter;
import org.nuist.minibase.scanner.SeekIter;
import org.nuist.minibase.wal.FileWalWriter;
import org.nuist.minibase.wal.IWalWriter;
import org.nuist.minibase.wal.WalFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

public class MemStore implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(MemStore.class);

  private final AtomicLong dataSize = new AtomicLong();

  private volatile ConcurrentSkipListMap<KeyValue, KeyValue> kvMap;
  private volatile ConcurrentSkipListMap<KeyValue, KeyValue> snapshot;

  private final ReentrantReadWriteLock updateLock = new ReentrantReadWriteLock();
  private final AtomicBoolean isSnapshotFlushing = new AtomicBoolean(false);
  private ExecutorService pool;
  private AtomicLong walId = new AtomicLong(1l);
  private Configuration conf;
  private Flusher flusher;
  FileWalWriter walWriter;
  private static final Pattern WAL_FILE_RE = Pattern.compile("(data[0-9]+)\\.wal$");

  public MemStore(Configuration conf, Flusher flusher, ExecutorService pool) throws Exception {
    this.conf = conf;
    this.flusher = flusher;
    this.pool = pool;

    dataSize.set(0);
    this.kvMap = new ConcurrentSkipListMap<>();
    this.snapshot = null;

    //加载wal旧的日志
    File[] files = listWalFiles();
    for (File f : files) {
      WalFileReader walFileReader = new WalFileReader(f.getPath());
      Iterator<KeyValue> iterator = walFileReader.read();
      while(iterator.hasNext()){

        KeyValue kv = iterator.next();
        addMem(kv);
      }
      walFileReader.close();
      //f.delete();
    }

    String walFileName = conf.getWalDir() + "/" + "data" + walId.getAndIncrement() + ".wal";
    walWriter = new FileWalWriter(walFileName);
  }

  public void addMem(KeyValue kv){
    updateLock.readLock().lock();
    try {
      KeyValue prevKeyValue;
      if ((prevKeyValue = kvMap.put(kv, kv)) == null) {
        dataSize.addAndGet(kv.getSerializeSize());
      } else {
        dataSize.addAndGet(kv.getSerializeSize() - prevKeyValue.getSerializeSize());
      }
    } finally {
      updateLock.readLock().unlock();
    }
  }

  public void add(KeyValue kv) throws Exception {
    flushIfNeeded(true);
    updateLock.readLock().lock();
    //写日志
    walWriter.write(kv);
    try {
      KeyValue prevKeyValue;
      if ((prevKeyValue = kvMap.put(kv, kv)) == null) {
        dataSize.addAndGet(kv.getSerializeSize());
      } else {
        dataSize.addAndGet(kv.getSerializeSize() - prevKeyValue.getSerializeSize());
      }
    } finally {
      updateLock.readLock().unlock();
    }
    flushIfNeeded(false);
  }

  private void flushIfNeeded(boolean shouldBlocking) throws IOException {
    if (getDataSize() > conf.getMaxMemstoreSize()) {
      if (isSnapshotFlushing.get() && shouldBlocking) {
        throw new IOException(
                "Memstore is full, currentDataSize=" + dataSize.get() + "B, maxMemstoreSize="
                + conf.getMaxMemstoreSize() + "B, please wait until the flushing is finished.");
      } else if (isSnapshotFlushing.compareAndSet(false, true)) {
        pool.submit(new FlusherTask());
      }
    }
  }

  public long getDataSize() {
    return dataSize.get();
  }

  public boolean isFlushing() {
    return this.isSnapshotFlushing.get();
  }

  @Override
  public void close() throws IOException {
  }

  private class FlusherTask implements Runnable {
    @Override
    public void run() {
      // Step.1 memstore snpashot
      updateLock.writeLock().lock();
      IWalWriter tmp = null;
      try {
        //关闭老的walWriter
        tmp = walWriter;
        tmp.close();
        walWriter = new FileWalWriter(conf.getWalDir() + "/" + "data" + walId.incrementAndGet() + ".wal");
        snapshot = kvMap;
        // TODO MemStoreIter may find the kvMap changed ? should synchronize ?
        kvMap = new ConcurrentSkipListMap<>();
        dataSize.set(0);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        updateLock.writeLock().unlock();
      }

      // Step.2 Flush the memstore to disk file.
      boolean success = false;
      for (int i = 0; i < conf.getFlushMaxRetries(); i++) {
        try {
          logger.info("start flush..." + System.currentTimeMillis());
          flusher.flush(new IteratorWrapper(snapshot));
          success = true;
          logger.info("finish flush..." + System.currentTimeMillis());
        } catch (IOException e) {
          logger.error("Failed to flush memstore, retries=" + i + ", maxFlushRetries="
                    + conf.getFlushMaxRetries(),
                  e);
          if (i >= conf.getFlushMaxRetries()) {
            break;
          }
        }
      }

      // Step.3 clear the snapshot.
      if (success) {
        // TODO MemStoreIter may get a NPE because we set null here ? should synchronize ?
        snapshot = null;
        isSnapshotFlushing.compareAndSet(true, false);
        //删除旧的日志文件
        new File(tmp.getWalFileName()).delete();
      }
    }
  }

  public SeekIter<KeyValue> createIterator() throws IOException {
    return new MemStoreIter(kvMap, snapshot);
  }


 //================================================================================================

  private File[] listWalFiles() {
    File f = new File(this.conf.getWalDir());
    return f.listFiles(fname -> WAL_FILE_RE.matcher(fname.getName()).matches());
  }
}
