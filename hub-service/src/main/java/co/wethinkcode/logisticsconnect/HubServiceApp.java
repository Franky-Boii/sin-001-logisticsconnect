package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.util.Map;

public class HubServiceApp {

    private static final String INGESTION_SERVICE_URL =
            System.getenv().getOrDefault("INGESTION_SERVICE_URL", "http://localhost:7050");

    public static void main(String[] args) {
        HubCache cache = new HubCache(new IngestionClient(INGESTION_SERVICE_URL).fetchHubs());

        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));

        // Serves provinces and sorting centers (place-name source of truth),
        // sourced from ingestion-service's cleaned output and cached at startup.
        app.get("/hubs", ctx -> ctx.json(cache.all()));

        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            cache.byId(hubId)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json(Map.of("error", "no hub with id " + hubId)));
        });

        app.get("/provinces", ctx -> ctx.json(cache.provinces()));
    }
}