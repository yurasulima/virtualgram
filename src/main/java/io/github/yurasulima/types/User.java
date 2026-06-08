package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
        @JsonProperty("id") long id,
        @JsonProperty("is_bot") boolean isBot,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("username") String username,
        @JsonProperty("language_code") String languageCode
) {
    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String username() {
        return username;
    }

    public String languageCode() {
        return languageCode;
    }

    public String displayName() {
        String first = firstName().trim();
        String ln = lastName();

        String last = ln != null && !ln.isBlank()
                ? ln.trim()
                : "";
        String full = (first + " " + last).trim();

        if (!full.isEmpty()) {
            return full;
        }
        if (!username().isBlank()) {
            return "@" + username();
        }
        return String.valueOf(id);
    }
}
