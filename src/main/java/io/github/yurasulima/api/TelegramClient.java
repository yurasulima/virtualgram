package io.github.yurasulima.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yurasulima.types.Message;
import io.github.yurasulima.types.Update;
import io.github.yurasulima.types.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin HTTP wrapper around the Telegram Bot API.
 * All methods are synchronous — call them from virtual threads.
 */
public final class TelegramClient {

    private static final String BASE = "https://api.telegram.org/bot";

    private final String token;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public TelegramClient(String token) {
        this(token, ObjectMapper::new);
    }

    /** For testing — inject a custom ObjectMapper factory. */
    public TelegramClient(String token, java.util.function.Supplier<ObjectMapper> mapperFactory) {
        this.token  = token;
        this.mapper = mapperFactory.get();
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // -------------------------------------------------------------------------
    // Polling
    // -------------------------------------------------------------------------

    /**
     * Calls getUpdates with long-polling.
     *
     * @param offset  pass (last_update_id + 1) to acknowledge previous updates
     * @param timeout long-poll timeout in seconds (0 = short poll)
     */
    public List<Update> getUpdates(long offset, int timeout) {
        String url = url("getUpdates") + "?offset=" + offset + "&timeout=" + timeout + "&limit=100";
        JsonNode result = get(url);
        List<Update> updates = new ArrayList<>();
        for (JsonNode node : result) {
            updates.add(mapper.convertValue(node, Update.class));
        }
        return updates;
    }

    // -------------------------------------------------------------------------
    // Bot info
    // -------------------------------------------------------------------------

    public User getMe() {
        return mapper.convertValue(get(url("getMe")), User.class);
    }

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    public Message sendMessage(SendMessage req) {
        validateOutgoingText(req.text(), "sendMessage");
        return mapper.convertValue(post("sendMessage", req), Message.class);
    }

    /** Shortcut — send plain text to a chat. */
    public Message sendText(long chatId, String text) {
        validateOutgoingText(text, "sendText");
        return sendMessage(SendMessage.to(chatId).text(text));
    }

    public void answerCallbackQuery(AnswerCallbackQuery req) {
        post("answerCallbackQuery", req);
    }

    public void answerCallbackQuery(String callbackQueryId) {
        answerCallbackQuery(AnswerCallbackQuery.of(callbackQueryId));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private String url(String method) {
        return BASE + token + "/" + method;
    }

    private JsonNode get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            return parseResponse(http.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (TelegramApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("HTTP GET failed: " + url, e);
        }
    }

    private JsonNode post(String method, Object body) {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url(method)))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return parseResponse(http.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (TelegramApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("HTTP POST failed: " + method, e);
        }
    }

    private JsonNode parseResponse(HttpResponse<String> response) throws Exception {
        JsonNode root = mapper.readTree(response.body());
        boolean ok = root.path("ok").asBoolean(false);
        if (!ok) {
            int    code = root.path("error_code").asInt(-1);
            String desc = root.path("description").asText("unknown error");
            throw new TelegramApiException(code, desc);
        }
        return root.get("result");
    }

    private void validateOutgoingText(String text, String method) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(method + " requires non-blank text");
        }
    }
}
