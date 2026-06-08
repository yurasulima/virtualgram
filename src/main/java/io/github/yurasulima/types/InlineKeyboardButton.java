package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InlineKeyboardButton(
        @JsonProperty("text")          String text,
        @JsonProperty("callback_data") String callbackData,
        @JsonProperty("url")           String url
) {
    public static InlineKeyboardButton callback(String text, String data) {
        return new InlineKeyboardButton(text, data, null);
    }

    public static InlineKeyboardButton url(String text, String url) {
        return new InlineKeyboardButton(text, null, url);
    }
}