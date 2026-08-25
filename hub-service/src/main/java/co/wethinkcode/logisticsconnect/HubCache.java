package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubDto;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

/**
 * In-memory query layer over the hub data fetched from ingestion-service.
 */
public class HubCache {

    private final List<HubDto> hubs;

    public HubCache(List<HubDto> hubs) {
        this.hubs = List.copyOf(hubs);
    }

    public List<HubDto> all() {
        return hubs;
    }

    public Optional<HubDto> byId(String hubId) {
        String normalized = hubId == null ? "" : hubId.trim().toUpperCase(Locale.ROOT);
        return hubs.stream()
                .filter(hub -> hub.hubId() != null && hub.hubId().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public List<String> provinces() {
        TreeSet<String> distinct = new TreeSet<>();
        for (HubDto hub : hubs) {
            if (hub.province() != null && !hub.province().isBlank()) {
                distinct.add(hub.province());
            }
        }
        return List.copyOf(distinct);
    }
}
