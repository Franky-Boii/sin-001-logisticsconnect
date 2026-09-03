package co.wethinkcode.logisticsconnect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import co.wethinkcode.logisticsconnect.model.HubRecord;

/**
 * Turns raw rows from hubs-global.csv into cleaned {@link HubRecord}s.
 * <p>
 * Deliberately separated from any CSV-parsing or Javalin/HTTP concern: this class
 * takes already-split rows (one {@code String[]} per data row, header excluded) so
 * the cleaning rules can be unit tested without touching a file or a server.
 * {@link IngestionServiceApp} is responsible for wiring opencsv's reader output into
 * this class, and for exposing the result over REST.
 * <p>
 * Expected column order per row, matching hubs-global.csv:
 * {@code [hub_id, province, sorting_center, active]}
 * <p>
 * Duplicate strategy: rows are keyed by normalized hub id, first-seen order is kept.
 * An exact repeat (same id, same other fields) collapses silently into one record.
 * A conflicting repeat (same id, different fields) keeps the first occurrence and
 * appends a note explaining what was discarded, rather than silently picking a
 * "winner" or emitting two records for the same real-world hub.
 */
public class HubCsvCleaner {

    private static final Set<String> TRUTHY = Set.of("y", "yes", "true", "1");
    private static final Set<String> FALSY = Set.of("n", "no", "false", "0");

    public List<HubRecord> clean(List<String[]> rawRows) {
        LinkedHashMap<String, HubRecord> byId = new LinkedHashMap<>();

        for (String[] row : rawRows) {
            HubRecord parsed = parseRow(row);
            HubRecord existing = byId.get(parsed.hubId());

            if (existing == null) {
                byId.put(parsed.hubId(), parsed);
            } else if (!sameCoreFields(existing, parsed)) {
                byId.put(parsed.hubId(), withConflictNote(existing, parsed));
            }
            // Exact duplicate of an already-seen hub — nothing to do.
        }

        return new ArrayList<>(byId.values());
    }

    private HubRecord parseRow(String[] row) {
        String rawId = field(row, 0);
        String rawProvince = field(row, 1);
        String rawSortingCenter = field(row, 2);
        String rawActive = field(row, 3);

        String hubId = normalizeId(rawId);
        List<String> notes = new ArrayList<>();

        String province = collapseWhitespace(rawProvince);

        if (province.isBlank()) {
            notes.add("missing province for hub " + hubId);
        } else {
            province = normalizeProvince(province);
        }

        String sortingCenter = titleCase(collapseWhitespace(rawSortingCenter));
        Boolean active = parseActive(rawActive, hubId, notes);

        return new HubRecord(
                hubId,
                province,
                sortingCenter,
                active,
                notes
        );
    }

    /**
     * Normalizes province names into a consistent canonical representation.
     *
     * In the source CSV, KwaZulu-Natal appears in several forms:
     * - KwaZulu-Natal
     * - Kwa-Zulu Natal
     * - KwaZulu Natal
     *
     * All are represented as "KwaZulu-Natal" in the cleaned data.
     */
    private String normalizeProvince(String raw) {
        String normalized = raw.trim().replaceAll("\\s+", " ");

        if (normalized.equalsIgnoreCase("KwaZulu-Natal")
                || normalized.equalsIgnoreCase("Kwa-Zulu Natal")
                || normalized.equalsIgnoreCase("KwaZulu Natal")) {
            return "KwaZulu-Natal";
        }

        return titleCase(normalized);
    }

    private Boolean parseActive(String raw, String hubId, List<String> notes) {
        String value = raw == null ? "" : raw.trim().toLowerCase();

        if (TRUTHY.contains(value)) {
            return Boolean.TRUE;
        }

        if (FALSY.contains(value)) {
            return Boolean.FALSE;
        }

        notes.add(
                "unrecognized active value '" + raw
                        + "' for hub " + hubId
                        + "; treated as unknown"
        );

        return null;
    }

    private boolean sameCoreFields(HubRecord a, HubRecord b) {
        return Objects.equals(a.province(), b.province())
                && Objects.equals(a.sortingCenter(), b.sortingCenter())
                && Objects.equals(a.active(), b.active());
    }

    private HubRecord withConflictNote(
            HubRecord existing,
            HubRecord conflicting
    ) {
        List<String> mergedNotes = new ArrayList<>(existing.notes());

        mergedNotes.add(
                "conflicting duplicate row for hub "
                        + existing.hubId()
                        + " discarded (province='"
                        + conflicting.province()
                        + "', sortingCenter='"
                        + conflicting.sortingCenter()
                        + "', active="
                        + conflicting.active()
                        + "); kept first occurrence"
        );

        return new HubRecord(
                existing.hubId(),
                existing.province(),
                existing.sortingCenter(),
                existing.active(),
                mergedNotes
        );
    }

    private String field(String[] row, int index) {
        return (row != null
                && index < row.length
                && row[index] != null)
                ? row[index]
                : "";
    }

    private String normalizeId(String raw) {
        return raw.trim().toUpperCase();
    }

    private String collapseWhitespace(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }

    private String titleCase(String s) {
        if (s.isBlank()) {
            return s;
        }

        StringBuilder result = new StringBuilder();

        for (String word : s.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(Character.toUpperCase(word.charAt(0)));

            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }
}

