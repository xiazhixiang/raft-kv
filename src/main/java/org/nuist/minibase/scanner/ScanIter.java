package org.nuist.minibase.scanner;

import org.nuist.minibase.KeyValue;
import org.nuist.minibase.utils.Bytes;

import java.io.IOException;

public class ScanIter implements Iter<KeyValue> {

    private KeyValue stopKV;
    private Iter<KeyValue> storeIt;
    // Last KV is the last key value which has the largest sequence id in key values with the
    // same key, but diff sequence id or op.
    private KeyValue lastKV = null;
    private KeyValue pendingKV = null;

    public ScanIter(KeyValue stopKV, SeekIter<KeyValue> it) {
        this.stopKV = stopKV;
        this.storeIt = it;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (pendingKV == null) {
            switchToNewKey();
        }
        return pendingKV != null;
    }

    private boolean shouldStop(KeyValue kv) {
        return stopKV != null && Bytes.compare(stopKV.getKey(), kv.getKey()) <= 0;
    }

    private void switchToNewKey() throws IOException {
        if (lastKV != null && shouldStop(lastKV)) {
            return;
        }
        KeyValue curKV;
        while (storeIt.hasNext()) {
            curKV = storeIt.next();
            if (shouldStop(curKV)) {
                return;
            }
            if (curKV.getOp() == KeyValue.Op.Put) {
                if (lastKV == null) {
                    lastKV = pendingKV = curKV;
                    return;
                }
                int ret = Bytes.compare(lastKV.getKey(), curKV.getKey());
                if (ret < 0) {
                    lastKV = pendingKV = curKV;
                    return;
                } else if (ret > 0) {
                    String msg = "KV mis-encoded, curKV < lastKV, curKV:" + Bytes.toHex(curKV.getKey()) +
                            ", lastKV:" + Bytes.toHex(lastKV.getKey());
                    throw new IOException(msg);
                }
                // Same key with lastKV, should continue to fetch the next key value.
            } else if (curKV.getOp() == KeyValue.Op.Delete) {
                if (lastKV == null || Bytes.compare(lastKV.getKey(), curKV.getKey()) != 0) {
                    lastKV = curKV;
                }
            } else {
                throw new IOException("Unknown op code: " + curKV.getOp());
            }
        }
    }

    @Override
    public KeyValue next() throws IOException {
        if (pendingKV == null) {
            switchToNewKey();
        }
        lastKV = pendingKV;
        pendingKV = null;
        return lastKV;
    }
}
