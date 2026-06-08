package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Chat(
        @JsonProperty("id")       long id,
        @JsonProperty("type")     String type,
        @JsonProperty("title")    String title,
        @JsonProperty("username") String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name")  String lastName
) {}