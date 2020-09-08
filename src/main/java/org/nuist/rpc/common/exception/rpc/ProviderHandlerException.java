package org.nuist.rpc.common.exception.rpc;

/**
 * @description 服务提供者端处理消费者的请求的时候，出现的异常
 */
public class ProviderHandlerException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5212501638591073686L;
	
	public ProviderHandlerException() {}

    public ProviderHandlerException(String message) {
        super(message);
    }

    public ProviderHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderHandlerException(Throwable cause) {
        super(cause);
    }
	
	

}
