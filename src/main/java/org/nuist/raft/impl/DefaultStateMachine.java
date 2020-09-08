package org.nuist.raft.impl;

import java.io.File;
import java.io.IOException;

import com.alibaba.fastjson.JSON;

import org.nuist.minibase.MStore;
import org.nuist.minibase.MiniBase;
import org.nuist.minibase.conf.Configuration;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import org.nuist.raft.entity.Command;
import org.nuist.raft.entity.LogEntry;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.nuist.raft.StateMachine;

/**
 *
 * 默认的状态机实现.
 *
 *
 */
public class DefaultStateMachine implements StateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStateMachine.class);

    /** public just for test */
    public static String dbDir;
    public static String stateMachineDir;

    //public static RocksDB machineDb;
    public static MiniBase machineDb;

    static {
        if (dbDir == null) {
            dbDir = "./rocksDB-raft/" + System.getProperty("serverPort");
        }
        if (stateMachineDir == null) {
            stateMachineDir =dbDir + "/stateMachine";
        }
        //RocksDB.loadLibrary();
    }


    public DefaultStateMachine() {
        synchronized (this) {
            try {
                File file = new File(stateMachineDir);
                boolean success = false;
                if (!file.exists()) {
                    success = file.mkdirs();
                }
                if (success) {
                    LOGGER.warn("make a new dir : " + stateMachineDir);
                }
                Configuration conf = new Configuration().setDataDir(stateMachineDir);
                machineDb = MStore.create(conf).open();

            } catch (RocksDBException e) {
                LOGGER.info(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static DefaultStateMachine getInstance() {
        return DefaultStateMachineLazyHolder.INSTANCE;
    }

    private static class DefaultStateMachineLazyHolder {

        private static final DefaultStateMachine INSTANCE = new DefaultStateMachine();
    }

    @Override
    public LogEntry get(String key) throws Exception{
        try {
            byte[] result = machineDb.get(key.getBytes()).toBytes();
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }

    @Override
    public String getString(String key) {
        try {
            byte[] bytes = machineDb.get(key.getBytes()).toBytes();
            if (bytes != null) {
                return new String(bytes);
            }
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        return "";
    }

    @Override
    public void setString(String key, String value) {
        try {
            machineDb.put(key.getBytes(), value.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delString(String... key) {
        try {
            for (String s : key) {
                machineDb.delete(s.getBytes());
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Override
    public synchronized void apply(LogEntry logEntry) {

        try {
            Command command = logEntry.getCommand();

            if (command == null) {
                throw new IllegalArgumentException("command can not be null, logEntry : " + logEntry.toString());
            }
            String key = command.getKey();
            machineDb.put(key.getBytes(), JSON.toJSONBytes(logEntry));
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

}
