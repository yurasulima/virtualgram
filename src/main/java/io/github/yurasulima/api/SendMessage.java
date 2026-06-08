package io.github.yurasulima.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.yurasulima.types.InlineKeyboardMarkup;

/**
 * Builder for the sendMessage API method.
 *
 * <pre>{@code
 * bot.send(SendMessage.to(chatId)
 *         .text("Hello!")
 *         .parseMode("HTML")
 *         .replyMarkup(keyboard));
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SendMessage {

    @JsonProperty("chat_id")              private final long chatId;
    @JsonProperty("text")                 private String text;
    @JsonProperty("parse_mode")           private String parseMode;
    @JsonProperty("reply_to_message_id")  private Long replyToMessageId;
    @JsonProperty("reply_markup")         private InlineKeyboardMarkup replyMarkup;
    @JsonProperty("disable_notification") private Boolean disableNotification;

    private SendMessage(long chatId) { this.chatId = chatId; }

    public static SendMessage to(long chatId)   { return new SendMessage(chatId); }

    public SendMessage text(String text)                          { this.text = text; return this; }
    public SendMessage parseMode(String mode)                     { this.parseMode = mode; return this; }
    public SendMessage replyTo(long messageId)                    { this.replyToMessageId = messageId; return this; }
    public SendMessage replyMarkup(InlineKeyboardMarkup markup)   { this.replyMarkup = markup; return this; }
    public SendMessage silent()                                   { this.disableNotification = true; return this; }

    // --- getters for serialisation ---
    public long chatId()                   { return chatId; }
    public String text()                   { return text; }
    public String parseMode()              { return parseMode; }
    public Long replyToMessageId()         { return replyToMessageId; }
    public InlineKeyboardMarkup replyMarkup() { return replyMarkup; }
    public Boolean disableNotification()   { return disableNotification; }
}