package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.yurasulima.Bot;
import io.github.yurasulima.api.AnswerCallbackQuery;
import io.github.yurasulima.handler.Context;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class CallbackQuery extends Context {

    private final String id;
    private final User from;
    private final Message message;
    private final String data;
    private final String chatInstance;

    @JsonCreator
    public CallbackQuery(
            @JsonProperty("id") String id,
            @JsonProperty("from") User from,
            @JsonProperty("message") Message message,
            @JsonProperty("data") String data,
            @JsonProperty("chat_instance") String chatInstance
    ) {
        this(id, from, message, data, chatInstance, null, null);
    }

    private CallbackQuery(String id, User from, Message message, String data, String chatInstance, Update update, Bot bot) {
        super(update, bot);
        this.id = id;
        this.from = from;
        this.message = message;
        this.data = data;
        this.chatInstance = chatInstance;
    }

    public static CallbackQuery contextOnly(Update update, Bot bot) {
        return new CallbackQuery(null, null, null, null, null, update, bot);
    }

    public CallbackQuery withContext(Update update, Bot bot) {
        Message contextualMessage = message != null ? message.withContext(update, bot) : null;
        return new CallbackQuery(id, from, contextualMessage, data, chatInstance, update, bot);
    }

    public String id() { return id; }
    @Override public User fromUser() { return from; }
    public User from() { return from; }
    @Override public Message message() { return message; }
    @Override public CallbackQuery callbackQuery() { return this; }
    public String data() { return data; }
    public String chatInstance() { return chatInstance; }

    public void answer(String notification) {
        ensureContext("answer");
        AnswerCallbackQuery req = AnswerCallbackQuery.of(id);
        if (notification != null && !notification.isBlank()) req.text(notification);
        bot.answerCallbackQuery(req);
    }

    public void answer() {
        answer(null);
    }
}
