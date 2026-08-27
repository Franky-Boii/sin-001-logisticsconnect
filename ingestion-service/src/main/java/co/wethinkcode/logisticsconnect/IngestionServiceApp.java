package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubRecord;
import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IngestionServiceApp {

    public static void main(String[] args) {
        List<HubRecord> hubs = loadAndCleanHubs();

        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // Cleaned hub records for other services (hub-service, etc.) to consume.
        app.get("/hubs", ctx -> ctx.json(hubs));

        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();
            HubRecord match = hubs.stream()
                    .filter(h -> h.hubId().equals(hubId))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                ctx.status(404).json(java.util.Map.of("error", "no hub with id " + hubId));
            } else {
                ctx.json(match);
            }
        });
    }

    private static List<HubRecord> loadAndCleanHubs() {
        try (InputStream in = IngestionServiceApp.class.getResourceAsStream("/hubs-global.csv")) {
            if (in == null) {
                throw new IllegalStateException("hubs-global.csv not found on classpath");
            }
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                List<String[]> allRows = csvReader.readAll();
                List<String[]> dataRows = allRows.isEmpty() ? allRows : allRows.subList(1, allRows.size());
                return new HubCsvCleaner().clean(dataRows);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load and clean hubs-global.csv", e);
        }
    }
}
