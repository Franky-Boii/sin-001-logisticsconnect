package co.wethinkcode.logisticsconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mirrors the JSON shape served by ingestion-service's {@code GET /hubs}
 * Kept as a separate type here rather than a shared dependency, since these are independent Maven
 * projects with no shared parent pom (same pattern as MqConfig in common/README.md).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubDto(String hubId, String province, String sortingCenter, Boolean active, List<String> notes) {
}