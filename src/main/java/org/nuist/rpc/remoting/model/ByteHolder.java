package org.nuist.rpc.remoting.model;

/**
 * 
 *
 * @description 
 * @time 8月9日
 * @
 */
public class ByteHolder {
	
	private transient byte[] bytes;

    public byte[] bytes() {
        return bytes;
    }

    public void bytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int size() {
        return bytes == null ? 0 : bytes.length;
    }

}
