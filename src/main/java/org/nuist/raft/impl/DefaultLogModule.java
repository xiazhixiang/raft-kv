package org.nuist.raft.impl;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;


import org.nuist.minibase.MStore;
import org.nuist.minibase.MiniBase;
import org.nuist.minibase.conf.Configuration;
import org.nuist.minibase.utils.Bytes;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import org.nuist.raft.entity.LogEntry;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nuist.raft.LogModule;
import lombok.Getter;
import lombok.Setter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 * 默认的日志实现. 日志模块不关心 key, 只关心 index.
 *
 *
 * @see LogEntry
 */
@Setter
@Getter
public class DefaultLogModule implements LogModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogModule.class);


    /** public just for test */
    public static String dbDir;
    public static String logsDir;

    //private static RocksDB logDb;

    private static MiniBase logDb;

    public final static byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    ReentrantLock lock = new ReentrantLock();

    static {
        if (dbDir == null) {
            dbDir = "./rocksDB-raft/" + System.getProperty("serverPort");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logModule";
        }
//        File f = new File(logsDir);
//        f.mkdirs();
        //RocksDB.loadLibrary();
    }

    public DefaultLogModule() {
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(logsDir);
        boolean success = false;
        if (!file.exists()) {
            success = file.mkdirs();
        }
        if (success) {
            LOGGER.warn("make a new dir : " + logsDir);
        }
        try {
            Configuration conf = new Configuration().setDataDir(logsDir);
            logDb = MStore.create(conf).open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DefaultLogModule getInstance() {
        return DefaultLogsLazyHolder.INSTANCE;
    }

    private static class DefaultLogsLazyHolder {

        private static final DefaultLogModule INSTANCE = new DefaultLogModule();
    }

    /**
     * logEntry 的 index 就是 key. 严格保证递增.
     *
     * @param logEntry
     */
    @Override
    public void write(LogEntry logEntry) throws Exception{

        boolean success = false;
        try {
            lock.tryLock(3000, MILLISECONDS);
            logEntry.setIndex(getLastIndex() + 1);
            logDb.put(logEntry.getIndex().toString().getBytes(), JSON.toJSONBytes(logEntry));
            success = true;
            LOGGER.info("DefaultLogModule write rocksDB success, logEntry info : [{}]", logEntry);
        } catch (RocksDBException | InterruptedException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(logEntry.getIndex());
            }
            lock.unlock();
        }
    }


    @Override
    public LogEntry read(Long index) {
        try {
            byte[] result = logDb.get(convert(index)).toBytes();
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void removeOnStartIndex(Long startIndex) throws Exception {
        boolean success = false;
        int count = 0;
        try {
            lock.tryLock(3000, MILLISECONDS);
            for (long i = startIndex; i <= getLastIndex(); i++) {
                logDb.delete(String.valueOf(i).getBytes());
                ++count;
            }
            success = true;
            LOGGER.warn("rocksDB removeOnStartIndex success, count={} startIndex={}, lastIndex={}", count, startIndex, getLastIndex());
        } catch (InterruptedException | IOException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(getLastIndex() - count);
            }
            lock.unlock();
        }
    }


    @Override
    public LogEntry getLast() {
        try {
            byte[] result = logDb.get(convert(getLastIndex())).toBytes();
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Long getLastIndex() {
        byte[] lastIndex = "-1".getBytes();
        try {
            lastIndex = logDb.get(LAST_INDEX_KEY) == null ? null : logDb.get(LAST_INDEX_KEY).toBytes();
            if (lastIndex == null) {
                lastIndex = "-1".getBytes();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Long.valueOf(new String(lastIndex));
    }

    private byte[] convert(Long key) {
        return key.toString().getBytes();
    }

    // on lock
    private void updateLastIndex(Long index) throws Exception {
        try {
            // overWrite
            logDb.put(LAST_INDEX_KEY, index.toString().getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }


}
