package org.nuist.minibase.wal;


import org.apache.commons.io.FileUtils;
import org.nuist.minibase.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * DefaultWalWriter
 * 默认的Wal的writer
 */

public class FileWalWriter implements IWalWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWalWriter.class);
    private String fileName;
    private FileOutputStream output;
    private File walFile;

    public FileWalWriter(String fileName) throws Exception {
        try{
            this.fileName = fileName;
            String dir = fileName.split("/")[0];
            File fileDir = new File(dir);
            if(!fileDir.exists())
                fileDir.mkdirs();
            walFile = new File(fileName);
            if(!walFile.exists()){
                if (!walFile.createNewFile()) {
                    throw new Exception("can not create file " + fileName);
                }
            }
            this.output = new FileOutputStream(walFile);
        }catch (Exception e){
            throw new Exception(e);
        }
    }

    @Override
    public void write(KeyValue kv) throws Exception {
        try {
            output.write(kv.toBytes());
            output.flush();
        }catch (IOException e){
            throw new Exception(e);
        }
    }

    @Override
    public void close() throws Exception {
        try {
            this.output.close();
        }catch (Exception e){
            throw new Exception(e);
        }
    }

    @Override
    public void delete() throws Exception {
        this.close();
        try{
            FileUtils.forceDelete(new File(fileName));
            LOGGER.info("delete wal success {}", fileName);
        }catch (Exception e){
            throw new Exception (e);
        }
    }

    @Override
    public String getWalFileName() {
        return fileName;
    }
}
