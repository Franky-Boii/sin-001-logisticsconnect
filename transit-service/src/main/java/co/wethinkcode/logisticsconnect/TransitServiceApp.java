package co.wethinkcode.logisticsconnect;

import java.util.Map;
import java.util.Optional;

import co.wethinkcode.logisticsconnect.model.HubDto;
import co.wethinkcode.logisticsconnect.mq.MqSubscriber;
import io.javalin.Javalin;

public class TransitServiceApp {

    private static final String HUB_SERVICE_URL =
            System.getenv().getOrDefault("HUB_SERVICE_URL", "http://localhost:7051");

    // Default stage for a hub this instance hasn't seen a package-status-topic
    // message for yet same default delay-stage-service
    // itself uses for an unset hub.
    private static final int DEFAULT_STAGE = 0;

    public static void main(String[] args) {
        HubServiceClient hubServiceClient = new HubServiceClient(HUB_SERVICE_URL);
        EtaCalculator etaCalculator = new EtaCalculator();

        // stage data now arrives via package-status-topic instead of a
        // synchronous call to delay-stage-service. DelayStageClient (the old sync
        // path) is left in the module for reference but is no longer called here.
        StageCache stageCache = new StageCache();
        MqSubscriber subscriber = new MqSubscriber(stageCache);
        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/eta/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId");
            Optional<HubDto> hub = hubServiceClient.fetchHub(hubId);

            if (hub.isEmpty()) {
                ctx.status(404).json(Map.of("error", "no hub with id " + hubId));
                return;
            }

            int delayStage = stageCache.stageFor(hubId, DEFAULT_STAGE);
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