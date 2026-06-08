package io.github.yurasulima.router;

import io.github.yurasulima.filters.Filter;
import io.github.yurasulima.filters.F;
import io.github.yurasulima.handler.Handler;
import io.github.yurasulima.handler.Context;
import io.github.yurasulima.types.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds an ordered list of (filter → handler) routes.
 * Routes are tested in registration order; the first match wins (like aiogram).
 *
 * <p>Routers can be nested: {@code dispatcher.include(router)}.
 *
 * <pre>{@code
 * var router = new Router();
 *
 * router.message(command("start"), ctx -> ctx.reply("Hello!"));
 * router.message(command("help"),  ctx -> ctx.reply("Help text"));
 * router.callbackQuery(callbackDataGlob("page_*"), ctx -> {
 *     ctx.answer();
 *     ctx.reply("Page: " + ctx.callbackQuery().data());
 * });
 * }</pre>
 */
public class Router {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    record Route(Filter filter, Handler handler) {}

    private final List<Route>  routes          = new ArrayList<>();
    private final List<Router> includedRouters = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Registration helpers
    // -------------------------------------------------------------------------

    /** Register a handler for any update matching the filter. */
    public Router on(Filter filter, Handler handler) {
        routes.add(new Route(filter, handler));
        return this;
    }

    /** Register a handler for message updates. */
    public Router message(Handler handler) {
        return on(F.hasMessage(), handler);
    }

    /** Register a handler for message updates that also pass the extra filter. */
    public Router message(Filter filter, Handler handler) {
        return on(F.hasMessage().and(filter), handler);
    }

    /** Register a handler for edited message updates. */
    public Router editedMessage(Filter filter, Handler handler) {
        return on(F.hasEditedMessage().and(filter), handler);
    }

    /** Register a handler for callback query updates. */
    public Router callbackQuery(Handler handler) {
        return on(F.hasCallbackQuery(), handler);
    }

    /** Register a handler for callback query updates that also pass the extra filter. */
    public Router callbackQuery(Filter filter, Handler handler) {
        return on(F.hasCallbackQuery().and(filter), handler);
    }

    /** Register a handler for inline query updates. */
    public Router inlineQuery(Filter filter, Handler handler) {
        return on(F.hasInlineQuery().and(filter), handler);
    }

    /** Include another router — its routes are checked after this router's own routes. */
    public Router include(Router other) {
        includedRouters.add(other);
        return this;
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    /**
     * Try to handle the update. Returns {@code true} if a handler was found and executed.
     */
    public boolean dispatch(Update update, Context ctx) {
        // Own routes first
        for (Route route : routes) {
            if (route.filter().test(update)) {
                executeHandler(route.handler(), ctx);
                return true;
            }
        }
        // Delegate to included routers
        for (Router child : includedRouters) {
            if (child.dispatch(update, ctx)) return true;
        }
        return false;
    }

    private void executeHandler(Handler handler, Context ctx) {
        try {
            handler.handle(ctx);
        } catch (Exception e) {
            log.error("Handler threw an exception for update {}: {}",
                    ctx.update().updateId(), e.getMessage(), e);
        }
    }
}
