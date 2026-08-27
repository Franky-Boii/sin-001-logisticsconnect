package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Fetches a single hub from hub-service's {@code GET /hubs/{hubId}}. Kept separate
 * from {@link EtaCalculator} so the ETA math stays unit-testable without a running
 * hub-service (see EtaCalculatorTest).
 */
public class HubServiceClient {

    private final String hubServiceBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HubServiceClient(String hubServiceBaseUrl) {
        this.hubServiceBaseUrl = hubServiceBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Empty if hub-service returns 404 for this hub id; throws for any other failure. */
    public Optional<HubDto> fetchHub(String hubId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(hubServiceBaseUrl + "/hubs/" + hubId))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "hub-service returned status " + response.statusCode() + " for GET /hubs/" + hubId);
            }
            return Optional.of(objectMapper.readValue(response.body(), HubDto.class));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "failed to fetch hub " + hubId + " from hub-service at " + hubServiceBaseUrl, e);
        }
    }
}
