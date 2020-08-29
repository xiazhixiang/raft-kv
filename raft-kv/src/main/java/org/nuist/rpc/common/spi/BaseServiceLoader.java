package org.nuist.rpc.common.spi;

import java.util.ServiceLoader;

/**
 * @description SPI loader
 */
public final class BaseServiceLoader {

    public static <S> S load(Class<S> serviceClass) {
        return ServiceLoader.load(serviceClass).iterator().next();
    }
}
