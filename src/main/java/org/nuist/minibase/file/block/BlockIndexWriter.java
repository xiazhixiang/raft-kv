package org.nuist.minibase.file.block;

import org.nuist.minibase.KeyValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockIndexWriter {

    private List<BlockMeta> blockMetas = new ArrayList<>();
    private int totalBytes = 0;

    public void append(KeyValue lastKV, long offset, long size, byte[] bloomFilter) {
        BlockMeta meta = new BlockMeta(lastKV, offset, size, bloomFilter);
        blockMetas.add(meta);
        totalBytes += meta.getSerializeSize();
    }

    public byte[] serialize() throws IOException {
        byte[] buffer = new byte[totalBytes];
        int pos = 0;
        for (BlockMeta meta : blockMetas) {
            byte[] metaBytes = meta.toBytes();
            System.arraycopy(metaBytes, 0, buffer, pos, metaBytes.length);
            pos += metaBytes.length;
        }
        assert pos == totalBytes;
        return buffer;
    }
}