package co.wethinkcode.logisticsconnect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.wethinkcode.logisticsconnect.mq.MqPublisher;
import io.javalin.Javalin;

public class DelayStageServiceApp {

    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;

    public static void main(String[] args) {
        Map<String, Integer> stages = new ConcurrentHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        MqPublisher publisher = new MqPublisher();
        Runtime.getRuntime().addShutdownHook(new Thread(publisher::close));

        Javalin app = Javalin.create().start(7052);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = normalizeHubId(ctx.pathParam("hubId"));
            if (hubId.isBlank()) {
                ctx.status(400).json(Map.of("error", "hubId must not be blank"));
                return;
            }
            ctx.json(Map.of("hubId", hubId, "stage", stages.getOrDefault(hubId, MIN_STAGE)));
        });

        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = normalizeHubId(ctx.pathParam("hubId"));
            if (hubId.isBlank()) {
                ctx.status(400).json(Map.of("error", "hubId must not be blank"));
                return;
            }

            JsonNode body;
            try {
                body = objectMapper.readTree(ctx.body());
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "request body must be valid JSON"));
                return;
            }

            JsonNode stageNode = body == null ? null : body.get("stage");
            if (stageNode == null || !stageNode.isInt()
                    || stageNode.intValue() < MIN_STAGE || stageNode.intValue() > MAX_STAGE) {
                ctx.status(400).json(Map.of("error", "stage must be an integer between 0 and 8"));
                return;
            }

            int stage = stageNode.intValue();
            stages.put(hubId, stage);

            // broadcast the change instead of making callers poll this
            // endpoint. A broker hiccup here is logged, not fatal — the state
            // change above already succeeded, and the point of decoupling is that
            // a publish failure shouldn't take the synchronous REST contract down.
            try {
                publisher.publishStageChange(hubId, stage);
            } catch (Exception mqError) {
                System.err.println("MqPublisher: failed to publish stage change for "
                        + hubId + ": " + mqError.getMessage());
            }

            ctx.status(200).json(Map.of("hubId", hubId, "stage", stage));
        });
    }

    private static String normalizeHubId(String hubId) {
        return hubId == null ? "" : hubId.trim().toUpperCase();
    }
}