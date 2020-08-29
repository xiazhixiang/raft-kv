package org.nuist.minibase.flush;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.file.DiskFileWriter;
import org.nuist.minibase.scanner.Iter;
import org.nuist.minibase.store.DiskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class DefaultFlusher implements Flusher {

    private static final String FILE_NAME_TMP_SUFFIX = ".tmp";
    private DiskStore diskStore;
    private static final Logger logger = LoggerFactory.getLogger(DefaultFlusher.class);

    public DefaultFlusher(DiskStore diskStore) {
        this.diskStore = diskStore;
    }

    @Override
    public void flush(Iter<KeyValue> it) throws IOException {
        String fileName = diskStore.getNextDiskFileName();
        String fileTempName = fileName + FILE_NAME_TMP_SUFFIX;
        try {
            try (DiskFileWriter writer = new DiskFileWriter(fileTempName)) {
                while (it.hasNext()) {
                    writer.append(it.next());
                }
                writer.appendIndex();
                writer.appendTrailer();
            }
            File f = new File(fileTempName);
            if (!f.renameTo(new File(fileName))) {
                throw new IOException(
                        "Rename " + fileTempName + " to " + fileName + " failed when flushing");
            }
            // TODO any concurrent issue ?
            diskStore.addDiskFile(fileName);
        } finally {
            File f = new File(fileTempName);
            if (f.exists()) {
                f.delete();
            }
        }
    }
}

