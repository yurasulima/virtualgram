package io.github.yurasulima.handler;

import io.github.yurasulima.Bot;
import io.github.yurasulima.api.SendMessage;
import io.github.yurasulima.api.TelegramClient;
import io.github.yurasulima.types.CallbackQuery;
import io.github.yurasulima.types.Chat;
import io.github.yurasulima.types.EditedMessage;
import io.github.yurasulima.types.InlineQuery;
import io.github.yurasulima.types.Message;
import io.github.yurasulima.types.Update;
import io.github.yurasulima.types.User;

/**
 * Base runtime context shared by rich update objects such as {@link io.github.yurasulima.types.Message}.
 */
public class Context {

    protected final Update update;
    protected final Bot bot;

    public Context(Update update, Bot bot) {
        this.update = update;
        this.bot = bot;
    }

    // -------------------------------------------------------------------------
    // Raw access
    // -------------------------------------------------------------------------

    public Update         update()        { return update; }
    public Bot            bot()           { return bot; }
    public TelegramClient client()        { return bot.client(); }

    // -------------------------------------------------------------------------
    // Generic accessors
    // -------------------------------------------------------------------------

    public Message message() {
        if (update == null) return null;
        if (update.hasMessage()) return update.message();
        if (update.hasChannelPost()) return update.channelPost();
        if (update.hasCallbackQuery() && update.callbackQuery().message() != null) {
            return update.callbackQuery().message();
        }
        return null;
    }

    public EditedMessage editedMessage() {
        if (update == null) return null;
        if (update.hasEditedMessage()) return update.editedMessage();
        if (update.hasEditedChannelPost()) return update.editedChannelPost();
        return null;
    }

    public CallbackQuery callbackQuery() {
        return update != null ? update.callbackQuery() : null;
    }

    public InlineQuery inlineQuery() {
        return update != null ? update.inlineQuery() : null;
    }

    public User fromUser() {
        return user();
    }

    public User user() {
        Message message = message();
        if (message != null && message.from() != null) return message.from();
        EditedMessage editedMessage = editedMessage();
        if (editedMessage != null && editedMessage.from() != null) return editedMessage.from();
        if (callbackQuery() != null) return callbackQuery().from();
        if (inlineQuery() != null) return inlineQuery().from();
        return null;
    }

    public Chat chat() {
        Message message = message();
        if (message != null && message.chat() != null) return message.chat();
        EditedMessage editedMessage = editedMessage();
        if (editedMessage != null && editedMessage.chat() != null) return editedMessage.chat();
        if (callbackQuery() != null && callbackQuery().message() != null) {
            return callbackQuery().message().chat();
        }
        return null;
    }

    public long chatId() {
        Chat chat = chat();
        if (chat != null) return chat.id();
        throw new IllegalStateException("No chat context for this update type");
    }

    public String text() {
        Message message = message();
        if (message != null && message.text() != null) return message.text();
        EditedMessage editedMessage = editedMessage();
        return editedMessage != null ? editedMessage.text() : null;
    }

    public String requireText() {
        String text = text();
        if (text != null) {
            return text;
        }
        throw new IllegalStateException("No text in the current update");
    }

    public Message reply(String text) {
        ensureContext("reply");
        return bot.sendText(chatId(), text);
    }

    public Message replyIfNotBlank(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return reply(text);
    }

    public Message replyQuote(String text) {
        ensureContext("replyQuote");
        Message current = message();
        SendMessage req = SendMessage.to(chatId()).text(text);
        if (current != null && current.messageId() > 0) {
            req.replyTo(current.messageId());
        }
        return bot.sendMessage(req);
    }

    public Message send(SendMessage req) {
        ensureContext("send");
        return bot.sendMessage(req);
    }

    protected void ensureContext(String method) {
        if (bot == null || update == null) {
            throw new IllegalStateException(method + " requires update context");
        }
    }
}
