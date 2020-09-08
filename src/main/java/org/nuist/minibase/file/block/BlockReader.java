package org.nuist.minibase.file.block;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.utils.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class BlockReader {

    private List<KeyValue> kvBuf;

    public BlockReader(List<KeyValue> kvBuf) {
        this.kvBuf = kvBuf;
    }

    public static BlockReader parseFrom(byte[] buffer, int offset, int size) throws IOException {
        int pos = 0;
        List<KeyValue> kvBuf = new ArrayList<KeyValue>();
        Checksum crc32 = new CRC32();

        // Parse kv getSerializeSize
        int kvSize = Bytes.toInt(Bytes.slice(buffer, offset + pos, BlockWriter.KV_SIZE_LEN));
        pos += BlockWriter.KV_SIZE_LEN;

        // Parse all key value.
        for (int i = 0; i < kvSize; i++) {
            KeyValue kv = KeyValue.parseFrom(buffer, offset + pos);
            kvBuf.add(kv);
            crc32.update(buffer, offset + pos, kv.getSerializeSize());
            pos += kv.getSerializeSize();
        }

        // Parse checksum
        int checksum = Bytes.toInt(Bytes.slice(buffer, offset + pos, BlockWriter.CHECKSUM_LEN));
        pos += BlockWriter.CHECKSUM_LEN;
        assert checksum == (int) (crc32.getValue() & 0xFFFFFFFF);

        assert pos == size : "pos: " + pos + ", getSerializeSize: " + size;

        return new BlockReader(kvBuf);
    }

    public List<KeyValue> getKeyValues() {
        return kvBuf;
    }
}
