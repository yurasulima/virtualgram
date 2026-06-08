package io.github.yurasulima.handler;

/**
 * A handler is a function that processes a rich {@link Context}.
 * It may throw any checked or unchecked exception — the dispatcher will catch and log it.
 */
@FunctionalInterface
public interface Handler {
    void handle(Context ctx) throws Exception;
}
