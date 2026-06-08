package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.yurasulima.Bot;
import io.github.yurasulima.handler.Context;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class EditedMessage extends Context {

    private final long messageId;
    private final User from;
    private final Chat chat;
    private final long date;
    private final String text;
    private final List<MessageEntity> entities;
    private final Message replyToMessage;
    private final Sticker sticker;
    private final List<PhotoSize> photo;
    private final Document document;
    private final String caption;

    @JsonCreator
    public EditedMessage(
            @JsonProperty("message_id") long messageId,
            @JsonProperty("from") User from,
            @JsonProperty("chat") Chat chat,
            @JsonProperty("date") long date,
            @JsonProperty("text") String text,
            @JsonProperty("entities") List<MessageEntity> entities,
            @JsonProperty("reply_to_message") Message replyToMessage,
            @JsonProperty("sticker") Sticker sticker,
            @JsonProperty("photo") List<PhotoSize> photo,
            @JsonProperty("document") Document document,
            @JsonProperty("caption") String caption
    ) {
        this(messageId, from, chat, date, text, entities, replyToMessage, sticker, photo, document, caption, null, null);
    }

    private EditedMessage(
            long messageId,
            User from,
            Chat chat,
            long date,
            String text,
            List<MessageEntity> entities,
            Message replyToMessage,
            Sticker sticker,
            List<PhotoSize> photo,
            Document document,
            String caption,
            Update update,
            Bot bot
    ) {
        super(update, bot);
        this.messageId = messageId;
        this.from = from;
        this.chat = chat;
        this.date = date;
        this.text = text;
        this.entities = entities;
        this.replyToMessage = replyToMessage;
        this.sticker = sticker;
        this.photo = photo;
        this.document = document;
        this.caption = caption;
    }

    public static EditedMessage contextOnly(Update update, Bot bot) {
        return new EditedMessage(0L, null, null, 0L, null, null, null, null, null, null, null, update, bot);
    }

    public EditedMessage withContext(Update update, Bot bot) {
        return new EditedMessage(
                messageId, from, chat, date, text, entities, replyToMessage, sticker, photo, document, caption, update, bot
        );
    }

    public long messageId() { return messageId; }
    public User from() { return from; }
    public Chat chat() { return chat; }
    public long date() { return date; }
    @Override public String text() { return text; }
    public List<MessageEntity> entities() { return entities; }
    public Message replyToMessage() { return replyToMessage; }
    public Sticker sticker() { return sticker; }
    public List<PhotoSize> photo() { return photo; }
    public Document document() { return document; }
    public String caption() { return caption; }

    public boolean isCommand(String command) {
        if (text == null) return false;
        String cmd = command.startsWith("/") ? command : "/" + command;
        return text.equals(cmd) || text.startsWith(cmd + " ") || text.startsWith(cmd + "@");
    }

    @Override
    public EditedMessage editedMessage() {
        return this;
    }

    public Message answer(String text) {
        return reply(text);
    }
}
