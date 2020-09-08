package org.nuist.rpc.common.serialization.proto;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuist.rpc.common.serialization.Serializer;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * @description 使用protoStuff序列化 
 * 序列化的对象不需要实现java.io.Serializable 也不需要有默认的构造函数
 */
public class ProtoStuffSerializer implements Serializer {
	
	private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();
	
	private static Objenesis objenesis = new ObjenesisStd(true);

	@SuppressWarnings("unchecked")
	public <T> byte[] writeObject(T obj) {
		
		Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
	}

	public <T> T readObject(byte[] bytes, Class<T> clazz) {
		try {
            T message = objenesis.newInstance(clazz);
            Schema<T> schema = getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(bytes, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
	}
	
	 @SuppressWarnings("unchecked")
	    private static <T> Schema<T> getSchema(Class<T> cls) {
	        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
	        if (schema == null) {
	            schema = RuntimeSchema.createFrom(cls);
	            cachedSchema.put(cls, schema);
	        }
	        return schema;
	    }

}
