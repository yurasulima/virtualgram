package io.github.yurasulima.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Sticker(
        @JsonProperty("file_id")        String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        @JsonProperty("width")          int width,
        @JsonProperty("height")         int height,
        @JsonProperty("emoji")          String emoji,
        @JsonProperty("set_name")       String setName
) {}