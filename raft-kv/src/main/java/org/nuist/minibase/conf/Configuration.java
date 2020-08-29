package org.nuist.minibase.conf;

public class Configuration {

  private long maxMemstoreSize = 16 * 1024 * 1024;
  private int flushMaxRetries = 10;
  private String dataDir = "MiniBase";
  private int maxDiskFiles = 10;
  private int maxThreadPoolSize = 5;
  private String walDir = "MiniWal";

  private static final Configuration DEFAULT = new Configuration();

  public Configuration setMaxMemstoreSize(long maxMemstoreSize) {
    this.maxMemstoreSize = maxMemstoreSize;
    return this;
  }

  public long getMaxMemstoreSize() {
    return this.maxMemstoreSize;
  }

  public Configuration setFlushMaxRetries(int flushMaxRetries) {
    this.flushMaxRetries = flushMaxRetries;
    return this;
  }

  public int getFlushMaxRetries() {
    return this.flushMaxRetries;
  }

  public Configuration setDataDir(String dataDir) {
    this.dataDir = dataDir;
    return this;
  }

  public String getDataDir() {
    return this.dataDir;
  }

  public Configuration setMaxDiskFiles(int maxDiskFiles) {
    this.maxDiskFiles = maxDiskFiles;
    return this;
  }

  public int getMaxDiskFiles() {
    return this.maxDiskFiles;
  }

  public Configuration setMaxThreadPoolSize(int maxThreadPoolSize) {
    this.maxThreadPoolSize = maxThreadPoolSize;
    return this;
  }

  public int getMaxThreadPoolSize() {
    return this.maxThreadPoolSize;
  }

  public static Configuration getDefault() {
    return DEFAULT;
  }

  public String getWalDir() {
    return walDir;
  }

  public void setWalDir(String walDir) {
    this.walDir = walDir;
  }
}
