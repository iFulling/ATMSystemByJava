package middleware;

/**
 * 中间件接口
 */
public interface Middleware {
    /**
     * 处理请求
     * @param context 请求上下文
     * @param chain 中间件链
     */
    void process(RequestContext context, MiddlewareChain chain);
} 