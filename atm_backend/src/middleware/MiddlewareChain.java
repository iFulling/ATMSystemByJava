package middleware;

import java.util.List;
import java.util.ArrayList;

/**
 * 中间件链，用于管理中间件的执行顺序
 */
public class MiddlewareChain {
    private final List<Middleware> middlewares;
    private int currentIndex;

    public MiddlewareChain() {
        this.middlewares = new ArrayList<>();
        this.currentIndex = 0;
    }

    public void addMiddleware(Middleware middleware) {
        middlewares.add(middleware);
    }

    public void next(RequestContext context) {
        if (currentIndex < middlewares.size()) {
            Middleware middleware = middlewares.get(currentIndex++);
            middleware.process(context, this);
        }
    }

    public void reset() {
        currentIndex = 0;
    }
} 