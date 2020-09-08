package org.nuist.minibase.wal;


import org.nuist.minibase.KeyValue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 */
public class WalFileReader implements IWalReader{
    private String path;
    private RandomAccessFile walFile;
    private MappedByteBuffer byteBuffer;
    public byte[] bytes;

    public WalFileReader(String path) throws IOException {
        this.path = path;
        walFile = new RandomAccessFile(path,"r");
        bytes = new byte[(int)walFile.length()];
        walFile.read(bytes);
//        byteBuffer = walFile.getChannel().map(FileChannel.MapMode.READ_ONLY,0,walFile.length());
//        byteBuffer.get(bytes);
    }

    @Override
    public Iterator<KeyValue> read() {
        return new Iterator<KeyValue>() {
            KeyValue currentCell = null;
            int pos = 0;
            @Override
            public boolean hasNext() {
                if(pos == bytes.length){
                    return false;
                }
                try {
                    currentCell = KeyValue.parseFrom(bytes, pos);
                    pos += currentCell.toBytes().length;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }

            @Override
            public KeyValue next() {
                return currentCell;
            }
        };
    }

    @Override
    public void close() throws IOException {
        this.walFile.close();
    }
}
