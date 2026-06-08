package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root Telegram Update object. Exactly one of the optional fields will be non-null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Update(
        @JsonProperty("update_id")      long updateId,
        @JsonProperty("message")        Message message,
        @JsonProperty("edited_message") EditedMessage editedMessage,
        @JsonProperty("channel_post")   Message channelPost,
        @JsonProperty("edited_channel_post") EditedMessage editedChannelPost,
        @JsonProperty("callback_query") CallbackQuery callbackQuery,
        @JsonProperty("inline_query")   InlineQuery inlineQuery
) {
    public boolean hasMessage()        { return message != null; }
    public boolean hasEditedMessage()  { return editedMessage != null; }
    public boolean hasChannelPost()    { return channelPost != null; }
    public boolean hasEditedChannelPost() { return editedChannelPost != null; }
    public boolean hasCallbackQuery()  { return callbackQuery != null; }
    public boolean hasInlineQuery()    { return inlineQuery != null; }
}
