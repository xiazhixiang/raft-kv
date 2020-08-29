package org.nuist.minibase.file.block;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.utils.Bytes;

import java.io.IOException;

public class BlockMeta implements Comparable<BlockMeta> {

    private static final int OFFSET_SIZE = 8;
    private static final int SIZE_SIZE = 8;
    private static final int BF_LEN_SIZE = 4;

    private KeyValue lastKV;
    private long blockOffset;
    private long blockSize;
    private byte[] bloomFilter;

    /**
     * @param lastKV the last key value to construct the dummy block meta.
     * @return the dummy block meta.
     */
    public static BlockMeta createSeekDummy(KeyValue lastKV) {
        return new BlockMeta(lastKV, 0L, 0L, Bytes.EMPTY_BYTES);
    }

    public BlockMeta(KeyValue lastKV, long offset, long size, byte[] bloomFilter) {
        this.lastKV = lastKV;
        this.blockOffset = offset;
        this.blockSize = size;
        this.bloomFilter = bloomFilter;
    }

    public KeyValue getLastKV() {
        return this.lastKV;
    }

    public long getBlockOffset() {
        return this.blockOffset;
    }

    public long getBlockSize() {
        return this.blockSize;
    }

    public byte[] getBloomFilter() {
        return this.bloomFilter;
    }

    public int getSerializeSize() {
        // TODO the meta no need the value of last kv, will save much bytes.
        return lastKV.getSerializeSize() + OFFSET_SIZE + SIZE_SIZE + BF_LEN_SIZE + bloomFilter.length;
    }

    public byte[] toBytes() throws IOException {
        byte[] bytes = new byte[getSerializeSize()];
        int pos = 0;

        // Encode last kv
        byte[] kvBytes = lastKV.toBytes();
        System.arraycopy(kvBytes, 0, bytes, pos, kvBytes.length);
        pos += kvBytes.length;

        // Encode blockOffset
        byte[] offsetBytes = Bytes.toBytes(blockOffset);
        System.arraycopy(offsetBytes, 0, bytes, pos, offsetBytes.length);
        pos += offsetBytes.length;

        // Encode blockSize
        byte[] sizeBytes = Bytes.toBytes(blockSize);
        System.arraycopy(sizeBytes, 0, bytes, pos, sizeBytes.length);
        pos += sizeBytes.length;

        // Encode length of bloom filter
        byte[] bfLenBytes = Bytes.toBytes(bloomFilter.length);
        System.arraycopy(bfLenBytes, 0, bytes, pos, bfLenBytes.length);
        pos += bfLenBytes.length;

        // Encode bytes of bloom filter.
        System.arraycopy(bloomFilter, 0, bytes, pos, bloomFilter.length);
        pos += bloomFilter.length;

        if (pos != bytes.length) {
            throw new IOException(
                    "pos(" + pos + ") should be equal to length of bytes (" + bytes.length + ")");
        }
        return bytes;
    }

    public static BlockMeta parseFrom(byte[] buf, int offset) throws IOException {
        int pos = offset;

        // Decode last key value.
        KeyValue lastKV = KeyValue.parseFrom(buf, offset);
        pos += lastKV.getSerializeSize();

        // Decode block blockOffset
        long blockOffset = Bytes.toLong(Bytes.slice(buf, pos, OFFSET_SIZE));
        pos += OFFSET_SIZE;

        // Decode block blockSize
        long blockSize = Bytes.toLong(Bytes.slice(buf, pos, SIZE_SIZE));
        pos += SIZE_SIZE;

        // Decode blockSize of block bloom filter
        int bloomFilterSize = Bytes.toInt(Bytes.slice(buf, pos, BF_LEN_SIZE));
        pos += BF_LEN_SIZE;

        // Decode bytes of block bloom filter
        byte[] bloomFilter = Bytes.slice(buf, pos, bloomFilterSize);
        pos += bloomFilterSize;

        assert pos <= buf.length;
        return new BlockMeta(lastKV, blockOffset, blockSize, bloomFilter);
    }

    @Override
    public int compareTo(BlockMeta o) {
        return this.lastKV.compareTo(o.lastKV);
    }
}