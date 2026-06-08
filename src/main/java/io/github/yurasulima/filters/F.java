package io.github.yurasulima.filters;

import io.github.yurasulima.types.Update;

import java.util.regex.Pattern;

/**
 * Factory class for common filters. Designed for static import:
 *
 * <pre>{@code
 * import static io.github.yurasulima.filters.F.*;
 *
 * router.message(command("start"), ctx -> ctx.reply("Hello!"));
 * router.message(text().and(chatType("private")), ctx -> ...);
 * router.callbackQuery(callbackData("buy_*"), ctx -> ...);
 * }</pre>
 */
public final class F {

    private F() {}

    // -------------------------------------------------------------------------
    // Update-level
    // -------------------------------------------------------------------------

    public static Filter hasMessage()       { return Update::hasMessage; }
    public static Filter hasEditedMessage() { return Update::hasEditedMessage; }
    public static Filter hasChannelPost()   { return Update::hasChannelPost; }
    public static Filter hasEditedChannelPost() { return Update::hasEditedChannelPost; }
    public static Filter hasCallbackQuery() { return Update::hasCallbackQuery; }
    public static Filter hasInlineQuery()   { return Update::hasInlineQuery; }

    // -------------------------------------------------------------------------
    // Message filters (require hasMessage)
    // -------------------------------------------------------------------------

    /** Matches a bot command like "/start". Strips the @botname suffix automatically. */
    public static Filter command(String cmd) {
        String normalized = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        return u -> u.hasMessage()
                && u.message().text() != null
                && u.message().isCommand(normalized);
    }

    /** Matches any message with non-null text. */
    public static Filter text() {
        return u -> u.hasMessage() && u.message().text() != null;
    }

    /** Matches message text exactly. */
    public static Filter text(String exact) {
        return u -> u.hasMessage() && exact.equals(u.message().text());
    }

    /** Matches message text against a regex pattern. */
    public static Filter text(Pattern pattern) {
        return u -> u.hasMessage()
                && u.message().text() != null
                && pattern.matcher(u.message().text()).matches();
    }

    /** Matches message text against a glob-style pattern (only * wildcard). */
    public static Filter text(String glob, boolean useGlob) {
        if (!useGlob) return text(glob);
        Pattern p = globToPattern(glob);
        return text(p);
    }

    /** Any message that contains a photo. */
    public static Filter photo() {
        return u -> hasPhoto(u.message()) || hasPhoto(u.editedMessage()) || hasPhoto(u.channelPost()) || hasPhoto(u.editedChannelPost());
    }

    /** Any message that contains a document. */
    public static Filter document() {
        return u -> hasDocument(u.message()) || hasDocument(u.editedMessage()) || hasDocument(u.channelPost()) || hasDocument(u.editedChannelPost());
    }

    /** Any message that contains a sticker. */
    public static Filter sticker() {
        return u -> hasSticker(u.message()) || hasSticker(u.editedMessage()) || hasSticker(u.channelPost()) || hasSticker(u.editedChannelPost());
    }

    /** Filter by chat type: "private", "group", "supergroup", "channel". */
    public static Filter chatType(String type) {
        return u -> u.hasMessage()
                && u.message().chat() != null
                && type.equals(u.message().chat().type());
    }

    public static Filter privateChat() { return chatType("private"); }
    public static Filter groupChat()   { return u -> {
        if (!u.hasMessage() || u.message().chat() == null) return false;
        String t = u.message().chat().type();
        return "group".equals(t) || "supergroup".equals(t);
    }; }

    /** Matches only messages from a specific user ID. */
    public static Filter fromUser(long userId) {
        return u -> u.hasMessage()
                && u.message().from() != null
                && u.message().from().id() == userId;
    }

    // -------------------------------------------------------------------------
    // CallbackQuery filters
    // -------------------------------------------------------------------------

    /** Matches callback_data exactly. */
    public static Filter callbackData(String data) {
        return u -> u.hasCallbackQuery() && data.equals(u.callbackQuery().data());
    }

    /**
     * Matches callback_data with a glob pattern (supports * wildcard).
     * Example: {@code callbackData("page_*")} matches "page_1", "page_home", etc.
     */
    public static Filter callbackDataGlob(String glob) {
        Pattern p = globToPattern(glob);
        return u -> u.hasCallbackQuery()
                && u.callbackQuery().data() != null
                && p.matcher(u.callbackQuery().data()).matches();
    }

    /** Matches callback_data with a regex. */
    public static Filter callbackDataPattern(Pattern pattern) {
        return u -> u.hasCallbackQuery()
                && u.callbackQuery().data() != null
                && pattern.matcher(u.callbackQuery().data()).matches();
    }

    // -------------------------------------------------------------------------
    // InlineQuery filters
    // -------------------------------------------------------------------------

    public static Filter inlineQuery() {
        return Update::hasInlineQuery;
    }

    public static Filter inlineQuery(String queryPrefix) {
        return u -> u.hasInlineQuery()
                && u.inlineQuery().query() != null
                && u.inlineQuery().query().startsWith(queryPrefix);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Pattern globToPattern(String glob) {
        String regex = "^" + Pattern.quote(glob).replace("\\*", ".*") + "$";
        return Pattern.compile(regex, Pattern.DOTALL);
    }

    private static boolean hasPhoto(io.github.yurasulima.types.Message message) {
        return message != null && message.photo() != null && !message.photo().isEmpty();
    }

    private static boolean hasPhoto(io.github.yurasulima.types.EditedMessage message) {
        return message != null && message.photo() != null && !message.photo().isEmpty();
    }

    private static boolean hasDocument(io.github.yurasulima.types.Message message) {
        return message != null && message.document() != null;
    }

    private static boolean hasDocument(io.github.yurasulima.types.EditedMessage message) {
        return message != null && message.document() != null;
    }

    private static boolean hasSticker(io.github.yurasulima.types.Message message) {
        return message != null && message.sticker() != null;
    }

    private static boolean hasSticker(io.github.yurasulima.types.EditedMessage message) {
        return message != null && message.sticker() != null;
    }
}
