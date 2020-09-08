package org.nuist.rpc.client.annotation;

import java.lang.annotation.*;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface RPConsumer {
	
	public String serviceName() default "";//服务名
	
}
