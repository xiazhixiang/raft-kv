package org.nuist.minibase.store;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.file.DiskFile;
import org.nuist.minibase.scanner.MultiIter;
import org.nuist.minibase.scanner.SeekIter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiskStore implements Cloneable{

    private static final Pattern DATA_FILE_RE = Pattern.compile("data\\.([0-9]+)");
    private static final Logger logger = LoggerFactory.getLogger(DiskStore.class);

    private String dataDir;
    private List<DiskFile> diskFiles;

    private int maxDiskFiles;
    private volatile AtomicLong maxFileId;

    public DiskStore(String dataDir, int maxDiskFiles) {
        this.dataDir = dataDir;
        this.diskFiles = new ArrayList<>();
        this.maxDiskFiles = maxDiskFiles;
    }

    public void open() throws IOException {
        File[] files = listDiskFiles();
        for (File f : files) {
            DiskFile df = new DiskFile();
            df.open(f.getAbsolutePath());
            diskFiles.add(df);
        }
        maxFileId = new AtomicLong(getMaxDiskId());
    }

    public synchronized String getNextDiskFileName() {
        return new File(this.dataDir, String.format("data.%020d", nextDiskFileId())).toString();
    }

    public synchronized void addDiskFile(String filename) throws IOException {
        DiskFile df = new DiskFile();
        df.open(filename);
        addDiskFile(df);
    }

    public void addDiskFile(DiskFile df) {
        synchronized (diskFiles) {
            diskFiles.add(df);
        }
    }

    public SeekIter<KeyValue> createIterator() throws IOException {
        return createIterator(getDiskFiles());
    }

    public List<DiskFile> getDiskFiles() {
        synchronized (diskFiles) {
            return new ArrayList<>(diskFiles);
        }
    }

    public void removeDiskFiles(Collection<DiskFile> files) {
        synchronized (diskFiles) {
            diskFiles.removeAll(files);
        }
    }

    public long getMaxDiskFiles() {
        return this.maxDiskFiles;
    }

    //================================私有函数=====================================================
    public synchronized long nextDiskFileId() {
        return maxFileId.incrementAndGet();
    }

    private File[] listDiskFiles() {
        File f = new File(this.dataDir);
        return f.listFiles(fname -> DATA_FILE_RE.matcher(fname.getName()).matches());
    }

    public synchronized long getMaxDiskId() {
        // TODO can we save the maxFileId ? and next time, need not to traverse the disk file.
        File[] files = listDiskFiles();
        long maxFileId = -1L;
        for (File f : files) {
            Matcher matcher = DATA_FILE_RE.matcher(f.getName());
            if (matcher.matches()) {
                maxFileId = Math.max(Long.parseLong(matcher.group(1)), maxFileId);
            }
        }
        return maxFileId;
    }

    public SeekIter<KeyValue> createIterator(List<DiskFile> diskFiles) throws IOException {
        List<SeekIter<KeyValue>> iters = new ArrayList<>();
        diskFiles.forEach(df -> iters.add(df.iterator()));
        return new MultiIter(iters);
    }
}
