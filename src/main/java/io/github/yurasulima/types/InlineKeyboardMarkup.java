package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

public record InlineKeyboardMarkup(
        @JsonProperty("inline_keyboard") List<List<InlineKeyboardButton>> inlineKeyboard
) {
    /** Convenience builder — each vararg is a row of buttons. */
    @SafeVarargs
    public static InlineKeyboardMarkup of(List<InlineKeyboardButton>... rows) {
        return new InlineKeyboardMarkup(Arrays.asList(rows));
    }

    public static List<InlineKeyboardButton> row(InlineKeyboardButton... buttons) {
        return Arrays.asList(buttons);
    }
}