package io.github.yurasulima;

import io.github.yurasulima.api.AnswerCallbackQuery;
import io.github.yurasulima.api.SendMessage;
import io.github.yurasulima.api.TelegramClient;
import io.github.yurasulima.types.Message;
import io.github.yurasulima.types.Update;
import io.github.yurasulima.types.User;

import java.util.List;

/**
 * High-level bot object, similar to aiogram's Bot.
 * Wraps the low-level {@link TelegramClient} and exposes convenient bot-centric methods.
 */
public final class Bot {

    private final String token;
    private final TelegramClient client;
    private volatile User cachedMe;

    public Bot(String token) {
        this.token = token;
        this.client = new TelegramClient(token);
    }

    public String token() {
        return token;
    }

    public TelegramClient client() {
        return client;
    }

    public User me() {
        User me = cachedMe;
        if (me == null) {
            me = getMe();
            cachedMe = me;
        }
        return me;
    }

    public User getMe() {
        return client.getMe();
    }

    public long id() {
        return me().id();
    }

    public String username() {
        return me().username();
    }

    public String firstName() {
        return me().firstName();
    }

    public List<Update> getUpdates(long offset, int timeout) {
        return client.getUpdates(offset, timeout);
    }

    public Message sendMessage(SendMessage req) {
        return client.sendMessage(req);
    }

    public Message sendText(long chatId, String text) {
        return client.sendText(chatId, text);
    }

    public void answerCallbackQuery(AnswerCallbackQuery req) {
        client.answerCallbackQuery(req);
    }

    public void answerCallbackQuery(String callbackQueryId) {
        client.answerCallbackQuery(callbackQueryId);
    }
}
