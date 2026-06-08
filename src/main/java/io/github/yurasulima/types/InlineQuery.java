package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.yurasulima.Bot;
import io.github.yurasulima.handler.Context;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class InlineQuery extends Context {

    private final String id;
    private final User from;
    private final String query;
    private final String offset;

    @JsonCreator
    public InlineQuery(
            @JsonProperty("id") String id,
            @JsonProperty("from") User from,
            @JsonProperty("query") String query,
            @JsonProperty("offset") String offset
    ) {
        this(id, from, query, offset, null, null);
    }

    private InlineQuery(String id, User from, String query, String offset, Update update, Bot bot) {
        super(update, bot);
        this.id = id;
        this.from = from;
        this.query = query;
        this.offset = offset;
    }

    public static InlineQuery contextOnly(Update update, Bot bot) {
        return new InlineQuery(null, null, null, null, update, bot);
    }

    public InlineQuery withContext(Update update, Bot bot) {
        return new InlineQuery(id, from, query, offset, update, bot);
    }

    public String id() { return id; }
    @Override public User fromUser() { return from; }
    public User from() { return from; }
    @Override public InlineQuery inlineQuery() { return this; }
    public String query() { return query; }
    public String offset() { return offset; }
}
