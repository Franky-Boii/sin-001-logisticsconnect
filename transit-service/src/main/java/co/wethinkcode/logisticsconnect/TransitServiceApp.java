package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubDto;
import io.javalin.Javalin;

import java.util.Map;
import java.util.Optional;

public class TransitServiceApp {

    private static final String HUB_SERVICE_URL =
            System.getenv().getOrDefault("HUB_SERVICE_URL", "http://localhost:7051");
    private static final String DELAY_STAGE_SERVICE_URL =
            System.getenv().getOrDefault("DELAY_STAGE_SERVICE_URL", "http://localhost:7052");

    public static void main(String[] args) {
        HubServiceClient hubServiceClient = new HubServiceClient(HUB_SERVICE_URL);
        DelayStageClient delayStageClient = new DelayStageClient(DELAY_STAGE_SERVICE_URL);
        EtaCalculator etaCalculator = new EtaCalculator();

        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        // Calculates estimated arrival windows based on hub and delay stage.
        // Stage 2 (this endpoint): calls hub-service and delay-stage-service
        // synchronously. Stage 3 replaces the delay-stage-service call with a
        // package-status-topic subscription — see DelayStageClient's note.
        app.get("/eta/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            Optional<HubDto> hub = hubServiceClient.fetchHub(hubId);

            if (hub.isEmpty()) {
                ctx.status(404).json(Map.of("error", "no hub with id " + hubId));
                return;
            }

            int delayStage = delayStageClient.fetchStage(hubId);
            int etaMinutes = etaCalculator.etaMinutes(delayStage);

            ctx.json(Map.of(
                    "hubId", hub.get().hubId(),
                    "province", hub.get().province(),
                    "sortingCenter", hub.get().sortingCenter(),
                    "delayStage", delayStage,
                    "etaMinutes", etaMinutes
            ));
        });
    }
}