package exceptions;

/**
 * @program: gateway-netty
 * @description: 未知协议的异常
 * @author: Havad
 * @create: 2025-02-14 15:44
 **/

public class UnsupportedProtocolException extends RuntimeException {
    public UnsupportedProtocolException(String message) {
        super(message);
    }

    public UnsupportedProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
