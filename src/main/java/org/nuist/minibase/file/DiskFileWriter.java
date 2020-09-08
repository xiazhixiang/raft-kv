package org.nuist.minibase.file;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.file.block.BlockIndexWriter;
import org.nuist.minibase.file.block.BlockWriter;
import org.nuist.minibase.utils.Bytes;

import java.io.*;

public class DiskFileWriter implements Closeable {
    private String fname;
    public static final int BLOCK_SIZE_UP_LIMIT = 1024 * 1024 * 2;

    private long currentOffset;
    private BlockIndexWriter indexWriter;
    private BlockWriter currentWriter;
    private FileOutputStream out;

    private long fileSize = 0;
    private int blockCount = 0;
    private long blockIndexOffset = 0;
    private long blockIndexSize = 0;

    public static final int TRAILER_SIZE = 8 + 4 + 8 + 8 + 8;
    public static final long DISK_FILE_MAGIC = 0xFAC881234221FFA9L;

    public DiskFileWriter(String fname) throws IOException {
        this.fname = fname;

        File f = new File(this.fname);
        f.createNewFile();
        out = new FileOutputStream(f, true);
        currentOffset = 0;
        indexWriter = new BlockIndexWriter();
        currentWriter = new BlockWriter();
    }

    public void append(KeyValue kv) throws IOException {
        if (kv == null) return;

        assert kv.getSerializeSize() + BlockWriter.KV_SIZE_LEN + BlockWriter.CHECKSUM_LEN < BLOCK_SIZE_UP_LIMIT;

        if ((currentWriter.getKeyValueCount() > 0)
                && (kv.getSerializeSize() + currentWriter.size() >= BLOCK_SIZE_UP_LIMIT)) {
            switchNextBlockWriter();
        }

        currentWriter.append(kv);
    }

    private void switchNextBlockWriter() throws IOException {
        assert currentWriter.getLastKV() != null;

        byte[] buffer = currentWriter.serialize();
        out.write(buffer);
        indexWriter.append(currentWriter.getLastKV(), currentOffset, buffer.length,
                currentWriter.getBloomFilter());

        currentOffset += buffer.length;
        blockCount += 1;

        // switch to the next block.
        currentWriter = new BlockWriter();
    }



    public void appendIndex() throws IOException {
        if (currentWriter.getKeyValueCount() > 0) {
            switchNextBlockWriter();
        }

        byte[] buffer = indexWriter.serialize();
        blockIndexOffset = currentOffset;
        blockIndexSize = buffer.length;

        out.write(buffer);

        currentOffset += buffer.length;
    }

    public void appendTrailer() throws IOException {
        fileSize = currentOffset + TRAILER_SIZE;

        // fileSize(8B)
        byte[] buffer = Bytes.toBytes(fileSize);
        out.write(buffer);

        // blockCount(4B)
        buffer = Bytes.toBytes(blockCount);
        out.write(buffer);

        // blockIndexOffset(8B)
        buffer = Bytes.toBytes(blockIndexOffset);
        out.write(buffer);

        // blockIndexSize(8B)
        buffer = Bytes.toBytes(blockIndexSize);
        out.write(buffer);

        // DISK_FILE_MAGIC(8B)
        buffer = Bytes.toBytes(DISK_FILE_MAGIC);
        out.write(buffer);
    }

    public void close() throws IOException {
        if (out != null) {
            try {
                out.flush();
                FileDescriptor fd = out.getFD();
                fd.sync();
            } finally {
                out.close();
            }
        }
    }
}
