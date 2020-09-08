package org.nuist.minibase;

import org.nuist.minibase.scanner.Iter;
import org.nuist.minibase.utils.Bytes;

import java.io.Closeable;
import java.io.IOException;

public interface MiniBase extends Closeable {

  void put(byte[] key, byte[] value) throws Exception;

  KeyValue get(byte[] key) throws IOException;

  void delete(byte[] key) throws IOException;

  Iter<KeyValue> scan(byte[] startKey, byte[] stopKey) throws IOException;

  default Iter<KeyValue> scan() throws IOException {
    return scan(Bytes.EMPTY_BYTES, Bytes.EMPTY_BYTES);
  }
}
