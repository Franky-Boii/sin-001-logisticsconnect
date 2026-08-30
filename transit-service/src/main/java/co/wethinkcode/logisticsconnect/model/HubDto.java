package co.wethinkcode.logisticsconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Mirrors the JSON shape served by hub-service's {@code GET /hubs/{hubId}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HubDto(String hubId, String province, String sortingCenter, Boolean active, List<String> notes) {
}
