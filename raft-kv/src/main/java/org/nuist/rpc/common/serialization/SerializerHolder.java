package org.nuist.rpc.common.serialization;

import org.nuist.rpc.common.spi.BaseServiceLoader;


/**
 * @description 序列化的入口,基于SPI方式
 */
public final class SerializerHolder {

    // SPI
    private static final Serializer serializer = BaseServiceLoader.load(Serializer.class);

    public static Serializer serializerImpl() {
        return serializer;
    }
}
