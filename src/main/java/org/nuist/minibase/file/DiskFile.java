package org.nuist.minibase.file;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.file.block.BlockMeta;
import org.nuist.minibase.file.block.BlockReader;
import org.nuist.minibase.scanner.SeekIter;
import org.nuist.minibase.utils.Bytes;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class DiskFile implements Closeable {

    public static final int TRAILER_SIZE = 8 + 4 + 8 + 8 + 8;
    public static final long DISK_FILE_MAGIC = 0xFAC881234221FFA9L;

    private String fname;
    private RandomAccessFile in;
    private SortedSet<BlockMeta> blockMetaSet = new TreeSet<>();

    private long fileSize;
    private int blockCount;
    private long blockIndexOffset;
    private long blockIndexSize;

    public void open(String filename) throws IOException {
        this.fname = filename;

        File f = new File(fname);
        this.in = new RandomAccessFile(f, "r");

        this.fileSize = f.length();
        assert fileSize > TRAILER_SIZE;
        in.seek(fileSize - TRAILER_SIZE);

        byte[] buffer = new byte[8];
        assert in.read(buffer) == buffer.length;
        assert this.fileSize == Bytes.toLong(buffer);

        buffer = new byte[4];
        assert in.read(buffer) == buffer.length;
        this.blockCount = Bytes.toInt(buffer);

        buffer = new byte[8];
        assert in.read(buffer) == buffer.length;
        this.blockIndexOffset = Bytes.toLong(buffer);

        buffer = new byte[8];
        assert in.read(buffer) == buffer.length;
        this.blockIndexSize = Bytes.toLong(buffer);

        buffer = new byte[8];
        assert in.read(buffer) == buffer.length;
        assert DISK_FILE_MAGIC == Bytes.toLong(buffer);

        // TODO Maybe a large memory, and overflow
        buffer = new byte[(int) blockIndexSize];
        in.seek(blockIndexOffset);
        assert in.read(buffer) == blockIndexSize;

        // TODO blockOffset may overflow.
        int offset = 0;

        do {
            BlockMeta meta = BlockMeta.parseFrom(buffer, offset);
            offset += meta.getSerializeSize();
            blockMetaSet.add(meta);
        } while (offset < buffer.length);

        assert blockMetaSet.size() == this.blockCount : "blockMetaSet.getSerializeSize:" + blockMetaSet.size()
                + ", blockCount: " + blockCount;
    }

    public SeekIter<KeyValue> iterator() {
        return new InternalIterator();
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }
    //===============================================================================================

    private BlockReader load(BlockMeta meta) throws IOException {
        in.seek(meta.getBlockOffset());

        // TODO Maybe overflow.
        byte[] buffer = new byte[(int) meta.getBlockSize()];

        assert in.read(buffer) == buffer.length;
        return BlockReader.parseFrom(buffer, 0, buffer.length);
    }

    public String getFileName() {
        return fname;
    }

//=================================================================================================
    private class InternalIterator implements SeekIter<KeyValue> {

        private int currentKVIndex = 0;
        private BlockReader currentReader;
        private Iterator<BlockMeta> blockMetaIter;

        public InternalIterator() {
            currentReader = null;
            blockMetaIter = blockMetaSet.iterator();
        }

        private boolean nextBlockReader() throws IOException {
            if (blockMetaIter.hasNext()) {
                currentReader = load(blockMetaIter.next());
                currentKVIndex = 0;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean hasNext() throws IOException {
            if (currentReader == null) {
                return nextBlockReader();
            } else {
                if (currentKVIndex < currentReader.getKeyValues().size()) {
                    return true;
                } else {
                    return nextBlockReader();
                }
            }
        }

        @Override
        public KeyValue next() throws IOException {
            return currentReader.getKeyValues().get(currentKVIndex++);
        }

        @Override
        public void seekTo(KeyValue target) throws IOException {
            // Locate the smallest block meta which has the lastKV >= target.
            blockMetaIter = blockMetaSet.tailSet(BlockMeta.createSeekDummy(target)).iterator();
            currentReader = null;
            if (blockMetaIter.hasNext()) {
                currentReader = load(blockMetaIter.next());
                currentKVIndex = 0;
                // Locate the smallest KV which is greater than or equals to the given KV. We're sure that
                // we can find the currentKVIndex, because lastKV of the block is greater than or equals
                // to the target KV.
                while (currentKVIndex < currentReader.getKeyValues().size()) {
                    KeyValue curKV = currentReader.getKeyValues().get(currentKVIndex);
                    if (curKV.compareTo(target) >= 0) {
                        break;
                    }
                    currentKVIndex++;
                }
                if (currentKVIndex >= currentReader.getKeyValues().size()) {
                    throw new IOException("Data block mis-encoded, lastKV of the currentReader >= kv, but " +
                            "we found all kv < target");
                }
            }
        }
    }


}
