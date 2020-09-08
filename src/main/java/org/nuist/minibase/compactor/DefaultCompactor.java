package org.nuist.minibase.compactor;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.file.DiskFile;
import org.nuist.minibase.file.DiskFileWriter;
import org.nuist.minibase.scanner.Iter;
import org.nuist.minibase.store.DiskStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultCompactor extends Compactor{

    private static final Logger logger = LoggerFactory.getLogger(DefaultCompactor.class);

    private DiskStore diskStore;
    private volatile boolean running = true;
    private static final String FILE_NAME_TMP_SUFFIX = ".tmp";
    private static final String FILE_NAME_ARCHIVE_SUFFIX = ".archive";

    public DefaultCompactor(DiskStore diskStore) {
        this.diskStore = diskStore;
        this.setDaemon(true);
    }

    @Override
    public void compact() throws IOException {
        List<DiskFile> filesToCompact = new ArrayList<>();
        filesToCompact.addAll(diskStore.getDiskFiles());
        performCompact(filesToCompact);
    }

    public void run() {
        while (running) {
            try {
                boolean isCompacted = false;
                if (diskStore.getDiskFiles().size() > diskStore.getMaxDiskFiles()) {
                    performCompact(diskStore.getDiskFiles());
                    isCompacted = true;
                }
                if (!isCompacted) {
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Major compaction failed: ", e);
            } catch (InterruptedException ie) {
                logger.error("InterruptedException happened, stop running: ", ie);
                break;
            }
        }
    }

    public void stopRunning() {
        this.running = false;
    }

    //========================================================================================================

    private void performCompact(List<DiskFile> filesToCompact) throws IOException {
        String fileName = diskStore.getNextDiskFileName();
        String fileTempName = fileName + FILE_NAME_TMP_SUFFIX;
        try {
            try (DiskFileWriter writer = new DiskFileWriter(fileTempName)) {
                for (Iter<KeyValue> it = diskStore.createIterator(filesToCompact); it.hasNext();) {
                    writer.append(it.next());
                }
                writer.appendIndex();
                writer.appendTrailer();
            }
            File f = new File(fileTempName);
            if (!f.renameTo(new File(fileName))) {
                throw new IOException("Rename " + fileTempName + " to " + fileName + " failed");
            }

            // Rename the data files to archive files.
            // TODO when rename the files, will we effect the scan ?
            for (DiskFile df : filesToCompact) {
                df.close();
                File file = new File(df.getFileName());
                File archiveFile = new File(df.getFileName() + FILE_NAME_ARCHIVE_SUFFIX);
                if (!file.renameTo(archiveFile)) {
                    logger.error("Rename " + df.getFileName() + " to " + archiveFile.getName() + " failed.");
                }
            }
            diskStore.removeDiskFiles(filesToCompact);

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
