package org.nuist.rpc.common.exception.rpc;

import java.net.SocketAddress;

/**
 *
 */
public class RemoteException extends RuntimeException {

    private static final long serialVersionUID = -6516335527982400712L;

    private final SocketAddress remoteAddress;

    public RemoteException(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public RemoteException(Throwable cause, SocketAddress remoteAddress) {
        super(cause);
        this.remoteAddress = remoteAddress;
    }

    public RemoteException(String message, SocketAddress remoteAddress) {
        super(message);
        this.remoteAddress = remoteAddress;
    }

    public RemoteException(String message, Throwable cause, SocketAddress remoteAddress) {
        super(message, cause);
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}
