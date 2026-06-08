package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEntity(
        @JsonProperty("type")   String type,
        @JsonProperty("offset") int offset,
        @JsonProperty("length") int length,
        @JsonProperty("url")    String url,
        @JsonProperty("user")   User user
) {}