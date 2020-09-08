package org.nuist.minibase.file.block;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.utils.BloomFilter;
import org.nuist.minibase.utils.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public  class BlockWriter {
    public static final int BLOCK_SIZE_UP_LIMIT = 1024 * 1024 * 2;
    public static final int BLOOM_FILTER_HASH_COUNT = 3;
    public static final int BLOOM_FILTER_BITS_PER_KEY = 10;

    public static final int KV_SIZE_LEN = 4;
    public static final int CHECKSUM_LEN = 4;

    private int totalSize;
    private List<KeyValue> kvBuf;
    private BloomFilter bloomFilter;
    private Checksum crc32;
    private KeyValue lastKV;
    private int keyValueCount;

    public BlockWriter() {
        totalSize = 0;
        kvBuf = new ArrayList<>();
        bloomFilter = new BloomFilter(BLOOM_FILTER_HASH_COUNT, BLOOM_FILTER_BITS_PER_KEY);
        crc32 = new CRC32();
    }

    public void append(KeyValue kv) throws IOException {
        // Update key value buffer
        kvBuf.add(kv);
        lastKV = kv;

        // Update checksum
        byte[] buf = kv.toBytes();
        crc32.update(buf, 0, buf.length);

        totalSize += kv.getSerializeSize();
        keyValueCount += 1;
    }

    public byte[] getBloomFilter() {
        byte[][] bytes = new byte[kvBuf.size()][];
        for (int i = 0; i < kvBuf.size(); i++) {
            bytes[i] = kvBuf.get(i).getKey();
        }
        return bloomFilter.generate(bytes);
    }

    public int getChecksum() {
        return (int) crc32.getValue();
    }

    public KeyValue getLastKV() {
        return this.lastKV;
    }

    public int size() {
        return KV_SIZE_LEN + totalSize + CHECKSUM_LEN;
    }

    public int getKeyValueCount() {
        return keyValueCount;
    }

    public byte[] serialize() throws IOException {
        byte[] buffer = new byte[size()];
        int pos = 0;

        // Append kv getSerializeSize.
        byte[] kvSize = Bytes.toBytes(kvBuf.size());
        System.arraycopy(kvSize, 0, buffer, pos, kvSize.length);
        pos += kvSize.length;

        // Append all the key value
        for (int i = 0; i < kvBuf.size(); i++) {
            byte[] kv = kvBuf.get(i).toBytes();
            System.arraycopy(kv, 0, buffer, pos, kv.length);
            pos += kv.length;
        }

        // Append checksum.
        byte[] checksum = Bytes.toBytes(this.getChecksum());
        System.arraycopy(checksum, 0, buffer, pos, checksum.length);
        pos += checksum.length;

        assert pos == size();
        return buffer;
    }
}
