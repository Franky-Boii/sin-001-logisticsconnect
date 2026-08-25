package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns raw rows from hubs-global.csv into cleaned {@link HubRecord}s.
 * <p>
 * Deliberately separated from any CSV-parsing or Javalin/HTTP concern: this class
 * takes already-split rows (one {@code String[]} per data row, header excluded) so
 * the cleaning *rules* can be unit tested without touching a file or a server.
 * {@link IngestionServiceApp} is responsible for wiring opencsv's reader output into
 * this class, and for exposing the result over REST.
 * <p>
 * Expected column order per row, matching hubs-global.csv:
 * {@code [hub_id, province, sorting_center, active]}
 *
 */
public class HubCsvCleaner {

    public List<HubRecord> clean(List<String[]> rawRows) {
        Map<String, HubRecord> recordsById = new LinkedHashMap<>();
        for (String[] row : rawRows) {
            if (row == null || row.length < 4) {
                continue;
            }

            String hubId = normalize(row[0]).toUpperCase(Locale.ROOT);
            String province = titleCase(normalize(row[1]));
            String sortingCenter = titleCase(normalize(row[2]));
            Boolean active = parseActive(row[3]);
            List<String> notes = new ArrayList<>();

            if (hubId.isEmpty()) {
                notes.add("missing hub id");
            }
            if (province.isEmpty()) {
                notes.add("missing province");
            }
            if (sortingCenter.isEmpty()) {
                notes.add("missing sorting center");
            }
            if (active == null) {
                notes.add("unrecognized active value: " + normalize(row[3]));
            }

            HubRecord record = new HubRecord(hubId, province, sortingCenter, active, notes);
            HubRecord existing = recordsById.putIfAbsent(hubId, record);
            if (existing != null && !existing.equals(record)) {
                List<String> mergedNotes = new ArrayList<>(existing.notes());
                mergedNotes.add("duplicate hub id resolved; first record retained");
                recordsById.put(hubId, new HubRecord(
                        existing.hubId(),
                        existing.province(),
                        existing.sortingCenter(),
                        existing.active(),
                        mergedNotes));
            }
        }
        return new ArrayList<>(recordsById.values());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (String word : value.toLowerCase(Locale.ROOT).split(" ")) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return result.toString();
    }

    private Boolean parseActive(String value) {
        return switch (normalize(value).toLowerCase(Locale.ROOT)) {
            case "y", "yes", "1", "true" -> true;
            case "n", "no", "0", "false" -> false;
            default -> null;
        };
    }
}
