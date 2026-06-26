package io.github.yurasulima;

import io.github.yurasulima.annotations.ByCommand;
import io.github.yurasulima.annotations.CallbackData;
import io.github.yurasulima.annotations.CallbackDataGlob;
import io.github.yurasulima.annotations.CallbackQueryHandler;
import io.github.yurasulima.annotations.ChannelPostHandler;
import io.github.yurasulima.annotations.Command;
import io.github.yurasulima.annotations.EditedMessageHandler;
import io.github.yurasulima.annotations.EditedChannelPostHandler;
import io.github.yurasulima.annotations.HasDocument;
import io.github.yurasulima.annotations.HasPhoto;
import io.github.yurasulima.annotations.InlineQueryHandler;
import io.github.yurasulima.annotations.InlineQueryStartsWith;
import io.github.yurasulima.annotations.MessageHandler;
import io.github.yurasulima.annotations.TextEquals;
import io.github.yurasulima.annotations.TextMatches;
import io.github.yurasulima.annotations.TextStartsWith;
import io.github.yurasulima.filters.F;
import io.github.yurasulima.filters.Filter;
import io.github.yurasulima.handler.Context;
import io.github.yurasulima.router.Router;
import io.github.yurasulima.types.CallbackQuery;
import io.github.yurasulima.types.EditedMessage;
import io.github.yurasulima.types.InlineQuery;
import io.github.yurasulima.types.Message;
import io.github.yurasulima.types.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The root dispatcher — extends {@link Router} and adds:
 * <ul>
 *   <li>Global error handler</li>
 *   <li>Middleware chain (pre/post processing)</li>
 *   <li>{@link #processUpdate} entry point called by the poller</li>
 * </ul>
 *
 * <pre>{@code
 * var dp = new Dispatcher();
 *
 * dp.onError((ctx, ex) -> ctx.reply("Oops, something went wrong."));
 *
 * dp.include(myRouter);
 * dp.message(command("start"), ctx -> ctx.reply("Hello!"));
 * }</pre>
 */
public class Dispatcher extends Router {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    private static final int DEFAULT_POLL_TIMEOUT_SECONDS = 30;
    private static final AtomicLong UPDATE_THREAD_ID = new AtomicLong();

    /** Called when a handler throws and no per-handler catch handles it. */
    private BiConsumer<Context, Throwable> errorHandler =
            (ctx, ex) -> log.error("Unhandled exception in update {}", ctx.update().updateId(), ex);

    private final List<Middleware> middlewares = new ArrayList<>();
    private final List<Consumer<Bot>> startupHooks = new ArrayList<>();
    private final List<Consumer<Bot>> shutdownHooks = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    public Dispatcher onError(BiConsumer<Context, Throwable> handler) {
        this.errorHandler = handler;
        return this;
    }

    /**
     * Add a middleware. Middlewares are executed in registration order before
     * the handler chain. A middleware can short-circuit by not calling {@code next.run()}.
     */
    public Dispatcher middleware(Middleware mw) {
        this.middlewares.add(mw);
        return this;
    }

    public Dispatcher onStartup(Consumer<Bot> hook) {
        this.startupHooks.add(hook);
        return this;
    }

    public Dispatcher onShutdown(Consumer<Bot> hook) {
        this.shutdownHooks.add(hook);
        return this;
    }

    public Dispatcher register(Object handlers) {
        int registered = 0;
        for (Method method : handlers.getClass().getDeclaredMethods()) {
            Filter filter = buildAnnotatedFilter(method);
            if (filter == null) {
                continue;
            }
            validateAnnotatedMethod(method);
            method.setAccessible(true);
            on(filter, ctx -> invokeAnnotatedMethod(handlers, method, ctx));
            registered++;
        }
        log.info("Registered {} annotated handler(s) from {}", registered, handlers.getClass().getName());
        return this;
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public void startPolling(Bot bot) {
        startPolling(bot, DEFAULT_POLL_TIMEOUT_SECONDS);
    }

    public void startPolling(Bot bot, int timeoutSeconds) {
        AtomicBoolean shutdownInvoked = new AtomicBoolean();
        Thread shutdownThread = Thread.ofPlatform()
                .name("vg-shutdown-hook")
                .unstarted(() -> invokeShutdownHooks(bot, shutdownInvoked));

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        try {
            invokeStartupHooks(bot);
            log.info("Bot @{} is up and waiting for updates", bot.username());

            long offset = 0L;
            // Створюємо прапорець для контролю життєвого циклу (корисно на майбутнє)
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<Update> updates = bot.getUpdates(offset, timeoutSeconds);
                    for (Update update : updates) {
                        offset = Math.max(offset, update.updateId() + 1L);
                        Thread.ofVirtual()
                                .name("vg-update-" + UPDATE_THREAD_ID.incrementAndGet())
                                .start(() -> processUpdate(update, bot));
                    }
                } catch (Exception e) {
                    // Логуємо помилку мережі, але НЕ даємо циклу while перерватися
                    log.error("Network error during long polling: {}. Retrying in 5 seconds...", e.getMessage());

                    // Робимо паузу перед наступним запитом, щоб не спамить Telegram у разі повної відсутності інтернету
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        log.info("Polling thread interrupted. Shutting down...");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            invokeShutdownHooks(bot, shutdownInvoked);
            removeShutdownHook(shutdownThread);
        }
    }

    /**
     * Process a single {@link Update}. Called by the poller for each incoming update,
     * typically on a virtual thread.
     */
    public void processUpdate(Update update, Bot bot) {
        Context ctx = contextualize(update, bot);
        long startedAt = System.nanoTime();
        try {
            boolean handled = runMiddleware(0, update, ctx);
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (handled) {
                log.info("Update {} handled in {} ms", update.updateId(), elapsedMs);
            } else {
                log.info("Update {} skipped in {} ms", update.updateId(), elapsedMs);
            }
        } catch (Throwable t) {
            errorHandler.accept(ctx, t);
        }
    }

    private boolean runMiddleware(int index, Update update, Context ctx) {
        if (index >= middlewares.size()) {
            return dispatch(update, ctx);
        }

        final boolean[] handled = {false};
        middlewares.get(index).invoke(ctx, () -> handled[0] = runMiddleware(index + 1, update, ctx));
        return handled[0];
    }

    // -------------------------------------------------------------------------
    // Middleware functional interface
    // -------------------------------------------------------------------------

    @FunctionalInterface
    public interface Middleware {
        /** Call {@code next.run()} to continue the chain. */
        void invoke(Context ctx, Runnable next);
    }

    private void invokeStartupHooks(Bot bot) {
        for (Consumer<Bot> hook : startupHooks) {
            hook.accept(bot);
        }
    }

    private void invokeShutdownHooks(Bot bot, AtomicBoolean shutdownInvoked) {
        if (!shutdownInvoked.compareAndSet(false, true)) {
            return;
        }

        for (Consumer<Bot> hook : shutdownHooks) {
            try {
                hook.accept(bot);
            } catch (Exception e) {
                log.error("Shutdown hook failed for bot @{}", bot.username(), e);
            }
        }
    }

    private void removeShutdownHook(Thread shutdownThread) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down, the hook is being or has been executed.
        }
    }

    private Filter buildAnnotatedFilter(Method method) {
        Filter base = null;
        int baseAnnotations = 0;

        if (method.isAnnotationPresent(MessageHandler.class)) {
            base = F.hasMessage();
            baseAnnotations++;
        }
        if (method.isAnnotationPresent(EditedMessageHandler.class)) {
            base = F.hasEditedMessage();
            baseAnnotations++;
        }
        if (method.isAnnotationPresent(ChannelPostHandler.class)) {
            base = F.hasChannelPost();
            baseAnnotations++;
        }
        if (method.isAnnotationPresent(EditedChannelPostHandler.class)) {
            base = F.hasEditedChannelPost();
            baseAnnotations++;
        }
        if (method.isAnnotationPresent(CallbackQueryHandler.class)) {
            base = F.hasCallbackQuery();
            baseAnnotations++;
        }
        if (method.isAnnotationPresent(InlineQueryHandler.class)) {
            base = F.hasInlineQuery();
            baseAnnotations++;
        }

        if (baseAnnotations == 0) {
            return null;
        }
        if (baseAnnotations > 1) {
            throw new IllegalArgumentException("Method " + method.getName() + " has multiple handler type annotations");
        }

        Filter filter = base;

        filter = applyHandlerAnnotationFilters(method, filter);
        filter = applyLegacyFilterAnnotations(method, filter);

        return filter;
    }

    private Filter applyHandlerAnnotationFilters(Method method, Filter filter) {
        MessageHandler messageHandler = method.getAnnotation(MessageHandler.class);
        if (messageHandler != null) {
            filter = applyMessageLikeFilters(
                    filter,
                    messageHandler.commands(),
                    messageHandler.prefix(),
                    messageHandler.textEquals(),
                    messageHandler.textStartsWith(),
                    messageHandler.textMatches(),
                    messageHandler.hasPhoto(),
                    messageHandler.hasDocument()
            );
        }

        EditedMessageHandler editedMessageHandler = method.getAnnotation(EditedMessageHandler.class);
        if (editedMessageHandler != null) {
            filter = applyMessageLikeFilters(
                    filter,
                    editedMessageHandler.command(),
                    editedMessageHandler.prefix(),
                    editedMessageHandler.textEquals(),
                    editedMessageHandler.textStartsWith(),
                    editedMessageHandler.textMatches(),
                    editedMessageHandler.hasPhoto(),
                    editedMessageHandler.hasDocument()
            );
        }

        ChannelPostHandler channelPostHandler = method.getAnnotation(ChannelPostHandler.class);
        if (channelPostHandler != null) {
            filter = applyMessageLikeFilters(
                    filter,
                    channelPostHandler.command(),
                    channelPostHandler.prefix(),
                    channelPostHandler.textEquals(),
                    channelPostHandler.textStartsWith(),
                    channelPostHandler.textMatches(),
                    channelPostHandler.hasPhoto(),
                    channelPostHandler.hasDocument()
            );
        }

        EditedChannelPostHandler editedChannelPostHandler = method.getAnnotation(EditedChannelPostHandler.class);
        if (editedChannelPostHandler != null) {
            filter = applyMessageLikeFilters(
                    filter,
                    editedChannelPostHandler.command(),
                    editedChannelPostHandler.prefix(),
                    editedChannelPostHandler.textEquals(),
                    editedChannelPostHandler.textStartsWith(),
                    editedChannelPostHandler.textMatches(),
                    editedChannelPostHandler.hasPhoto(),
                    editedChannelPostHandler.hasDocument()
            );
        }

        CallbackQueryHandler callbackQueryHandler = method.getAnnotation(CallbackQueryHandler.class);
        if (callbackQueryHandler != null) {
            if (callbackQueryHandler.data().length > 0) {
                filter = filter.and(anyOf(callbackQueryHandler.data(), F::callbackData));
            }
            if (callbackQueryHandler.dataGlob().length > 0) {
                filter = filter.and(anyOf(callbackQueryHandler.dataGlob(), F::callbackDataGlob));
            }
        }

        InlineQueryHandler inlineQueryHandler = method.getAnnotation(InlineQueryHandler.class);
        if (inlineQueryHandler != null && inlineQueryHandler.queryStartsWith().length > 0) {
            filter = filter.and(update -> {
                if (!update.hasInlineQuery() || update.inlineQuery().query() == null) return false;
                return Arrays.stream(inlineQueryHandler.queryStartsWith())
                        .anyMatch(prefix -> update.inlineQuery().query().startsWith(prefix));
            });
        }

        return filter;
    }

    private Filter applyLegacyFilterAnnotations(Method method, Filter filter) {
        Command command = method.getAnnotation(Command.class);
        if (command != null) {
            filter = filter.and(commandFilter(command.commands(), command.prefix(), "@Command"));
        }

        ByCommand byCommand = method.getAnnotation(ByCommand.class);
        if (byCommand != null) {
            filter = filter.and(commandFilter(byCommand.command(), byCommand.prefix(), "@ByCommand"));
        }

        TextEquals textEquals = method.getAnnotation(TextEquals.class);
        if (textEquals != null) {
            filter = filter.and(textEqualsFilter(textEquals.value()));
        }

        TextStartsWith textStartsWith = method.getAnnotation(TextStartsWith.class);
        if (textStartsWith != null) {
            filter = filter.and(textStartsWithFilter(textStartsWith.value()));
        }

        TextMatches textMatches = method.getAnnotation(TextMatches.class);
        if (textMatches != null) {
            filter = filter.and(textMatchesFilter(textMatches.value()));
        }

        if (method.isAnnotationPresent(HasPhoto.class)) {
            filter = filter.and(F.photo());
        }

        if (method.isAnnotationPresent(HasDocument.class)) {
            filter = filter.and(F.document());
        }

        CallbackData callbackData = method.getAnnotation(CallbackData.class);
        if (callbackData != null) {
            filter = filter.and(anyOf(callbackData.value(), F::callbackData));
        }

        CallbackDataGlob callbackDataGlob = method.getAnnotation(CallbackDataGlob.class);
        if (callbackDataGlob != null) {
            filter = filter.and(anyOf(callbackDataGlob.value(), F::callbackDataGlob));
        }

        InlineQueryStartsWith inlineQueryStartsWith = method.getAnnotation(InlineQueryStartsWith.class);
        if (inlineQueryStartsWith != null) {
            filter = filter.and(update -> {
                if (!update.hasInlineQuery() || update.inlineQuery().query() == null) return false;
                return Arrays.stream(inlineQueryStartsWith.value())
                        .anyMatch(prefix -> update.inlineQuery().query().startsWith(prefix));
            });
        }

        return filter;
    }

    private Filter applyMessageLikeFilters(
            Filter filter,
            String[] commands,
            String prefixes,
            String[] textEquals,
            String[] textStartsWith,
            String[] textMatches,
            boolean hasPhoto,
            boolean hasDocument
    ) {
        if (commands.length > 0) {
            filter = filter.and(commandFilter(commands, prefixes, "handler annotation"));
        }
        if (textEquals.length > 0) {
            filter = filter.and(textEqualsFilter(textEquals));
        }
        if (textStartsWith.length > 0) {
            filter = filter.and(textStartsWithFilter(textStartsWith));
        }
        if (textMatches.length > 0) {
            filter = filter.and(textMatchesFilter(textMatches));
        }
        if (hasPhoto) {
            filter = filter.and(F.photo());
        }
        if (hasDocument) {
            filter = filter.and(F.document());
        }
        return filter;
    }

    private void validateAnnotatedMethod(Method method) {
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException(
                    "Annotated handler method " + method.getName() + " must accept exactly one parameter"
            );
        }

        Class<?> parameterType = method.getParameterTypes()[0];
        Class<? extends Context> expectedType = expectedContextType(method);
        if (!parameterType.isAssignableFrom(expectedType)) {
            throw new IllegalArgumentException(
                    "Annotated handler method " + method.getName()
                            + " must accept " + expectedType.getSimpleName()
                            + " (or one of its supertypes like Context)"
            );
        }
    }

    private void invokeAnnotatedMethod(Object handlers, Method method, Context ctx) throws Exception {
        try {
            method.invoke(handlers, ctx);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new RuntimeException("Failed to invoke annotated handler " + method.getName(), e);
        }
    }

    private Filter anyOf(String[] values, Function<String, Filter> mapper) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Filter annotation must declare at least one value");
        }
        Filter filter = mapper.apply(values[0]);
        for (int i = 1; i < values.length; i++) {
            filter = filter.or(mapper.apply(values[i]));
        }
        return filter;
    }

    private Filter commandFilter(String[] commands, String prefixes, String annotationName) {
        if (commands.length == 0) {
            throw new IllegalArgumentException(annotationName + " must declare at least one command");
        }
        if (prefixes == null || prefixes.isEmpty()) {
            throw new IllegalArgumentException(annotationName + " must declare at least one prefix character");
        }

        return update -> {
            String text = extractMessageText(update);
            if (text == null || text.isBlank()) {
                return false;
            }

            char prefix = text.charAt(0);
            if (prefixes.indexOf(prefix) < 0) {
                return false;
            }

            String body = text.substring(1);
            for (String command : commands) {
                if (matchesCommandBody(body, command)) {
                    return true;
                }
            }
            return false;
        };
    }

    private Filter textEqualsFilter(String[] values) {
        return update -> {
            String text = extractMessageText(update);
            return text != null && Arrays.stream(values).anyMatch(text::equals);
        };
    }

    private Filter textStartsWithFilter(String[] values) {
        return update -> {
            String text = extractMessageText(update);
            return text != null && Arrays.stream(values).anyMatch(text::startsWith);
        };
    }

    private Filter textMatchesFilter(String[] values) {
        Pattern[] patterns = Arrays.stream(values).map(Pattern::compile).toArray(Pattern[]::new);
        return update -> {
            String text = extractMessageText(update);
            return text != null && Arrays.stream(patterns).anyMatch(pattern -> pattern.matcher(text).matches());
        };
    }

    private boolean matchesCommandBody(String body, String command) {
        String normalized = command.startsWith("/") || command.startsWith("!") || command.startsWith(".")
                ? command.substring(1)
                : command;

        return body.equals(normalized)
                || body.startsWith(normalized + " ")
                || body.startsWith(normalized + "@");
    }

    private String extractMessageText(Update update) {
        if (update.hasMessage()) {
            return update.message().text();
        }
        if (update.hasEditedMessage()) {
            return update.editedMessage().text();
        }
        if (update.hasChannelPost()) {
            return update.channelPost().text();
        }
        if (update.hasEditedChannelPost()) {
            return update.editedChannelPost().text();
        }
        return null;
    }

    private Context contextualize(Update update, Bot bot) {
        if (update.hasMessage()) {
            return update.message().withContext(update, bot);
        }
        if (update.hasEditedMessage()) {
            return update.editedMessage().withContext(update, bot);
        }
        if (update.hasChannelPost()) {
            return update.channelPost().withContext(update, bot);
        }
        if (update.hasEditedChannelPost()) {
            return update.editedChannelPost().withContext(update, bot);
        }
        if (update.hasCallbackQuery()) {
            return update.callbackQuery().withContext(update, bot);
        }
        if (update.hasInlineQuery()) {
            return update.inlineQuery().withContext(update, bot);
        }
        return Message.contextOnly(update, bot);
    }

    private Class<? extends Context> expectedContextType(Method method) {
        if (method.isAnnotationPresent(MessageHandler.class) || method.isAnnotationPresent(ChannelPostHandler.class)) {
            return Message.class;
        }
        if (method.isAnnotationPresent(EditedMessageHandler.class) || method.isAnnotationPresent(EditedChannelPostHandler.class)) {
            return EditedMessage.class;
        }
        if (method.isAnnotationPresent(CallbackQueryHandler.class)) {
            return CallbackQuery.class;
        }
        if (method.isAnnotationPresent(InlineQueryHandler.class)) {
            return InlineQuery.class;
        }
        return Context.class;
    }
}
