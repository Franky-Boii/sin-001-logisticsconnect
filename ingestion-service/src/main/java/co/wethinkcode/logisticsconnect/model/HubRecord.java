package co.wethinkcode.logisticsconnect.model;

import java.util.List;
import java.util.Objects;

/**
 * A single cleaned hub record, ready to be served over REST by IngestionServiceApp
 * and consumed by hub-service.
 *
 * {@code active} is nullable: a raw value like "unknown" or "N/A" should end up as
 * {@code null} here (not defaulted to true/false), with the reason captured in
 * {@code notes} — see the ingestion-service README's "Known data issues" list.
 */
public final class HubRecord {

    private final String hubId;
    private final String province;
    private final String sortingCenter;
    private final Boolean active;
    private final List<String> notes;

    public HubRecord(String hubId, String province, String sortingCenter, Boolean active, List<String> notes) {
        this.hubId = hubId;
        this.province = province;
        this.sortingCenter = sortingCenter;
        this.active = active;
        this.notes = List.copyOf(notes);
    }

    public String hubId() {
        return hubId;
    }

    public String province() {
        return province;
    }

    public String sortingCenter() {
        return sortingCenter;
    }

    public Boolean active() {
        return active;
    }

    public List<String> notes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HubRecord other)) return false;
        return Objects.equals(hubId, other.hubId)
                && Objects.equals(province, other.province)
                && Objects.equals(sortingCenter, other.sortingCenter)
                && Objects.equals(active, other.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hubId, province, sortingCenter, active);
    }

    @Override
    public String toString() {
        return "HubRecord{hubId='%s', province='%s', sortingCenter='%s', active=%s, notes=%s}"
                .formatted(hubId, province, sortingCenter, active, notes);
    }
}
