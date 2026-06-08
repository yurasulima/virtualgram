package io.github.yurasulima.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AnswerCallbackQuery {

    @JsonProperty("callback_query_id") private final String callbackQueryId;
    @JsonProperty("text")              private String text;
    @JsonProperty("show_alert")        private Boolean showAlert;

    private AnswerCallbackQuery(String id) { this.callbackQueryId = id; }

    public static AnswerCallbackQuery of(String callbackQueryId) {
        return new AnswerCallbackQuery(callbackQueryId);
    }

    public AnswerCallbackQuery text(String text)   { this.text = text; return this; }
    public AnswerCallbackQuery alert()             { this.showAlert = true; return this; }

    public String callbackQueryId() { return callbackQueryId; }
    public String text()            { return text; }
    public Boolean showAlert()      { return showAlert; }
}