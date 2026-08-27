package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Fetches cleaned hub records from ingestion-service's {@code GET /hubs}. This is
 * the I/O boundary of the module — kept separate from {@link HubCache} so the query
 * logic can be unit tested without a running ingestion-service (see HubCacheTest).
 */
public class IngestionClient {

    private final String ingestionServiceBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestionClient(String ingestionServiceBaseUrl) {
        this.ingestionServiceBaseUrl = ingestionServiceBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<HubDto> fetchHubs() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ingestionServiceBaseUrl + "/hubs"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "ingestion-service returned status " + response.statusCode() + " for GET /hubs");
            }
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, HubDto.class));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "failed to fetch hubs from ingestion-service at " + ingestionServiceBaseUrl, e);
        }
    }
}
