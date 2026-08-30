package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches the current delay stage from delay-stage-service's
 * {@code GET /delay-stage/{hubId}}.
 * <p>
 * Stage 3 note: once MQ decoupling is in place, TransitServiceApp should stop
 * calling this synchronously and instead read the last stage seen on
 * {@code package-status-topic} — this class becomes the "before" half of that
 * before/after comparison in a debrief.
 */
public class DelayStageClient {

    private final String delayStageServiceBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DelayStageClient(String delayStageServiceBaseUrl) {
        this.delayStageServiceBaseUrl = delayStageServiceBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public int fetchStage(String hubId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(delayStageServiceBaseUrl + "/delay-stage/" + hubId))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "delay-stage-service returned status " + response.statusCode()
                                + " for GET /delay-stage/" + hubId);
            }
            JsonNode json = objectMapper.readTree(response.body());
            return json.get("stage").asInt();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "failed to fetch delay stage for " + hubId + " from " + delayStageServiceBaseUrl, e);
        }
    }
}
