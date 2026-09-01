package co.wethinkcode.logisticsconnect;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Decides whether a package-status-topic message warrants a simulated alert, and
 * records the ones that do. Deliberately free of any JMS type — MqSubscriber owns
 * the broker connection and hands this class raw message bodies, which is what
 * makes this class testable without a running ActiveMQ broker (see
 * AlertProcessorTest).
 * <p>
 * ALERT_THRESHOLD is a judgment call, same as EtaCalculator's per-stage minutes in
 * transit-service: stage 6+ (out of 0-8) is treated as "severe enough to post
 * about" here. Worth being able to defend in a debrief, not treat as settled fact.
 */
public class AlertProcessor {

    public static final int ALERT_THRESHOLD = 6;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    public void processMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode hubIdNode = node.get("hubId");
            JsonNode stageNode = node.get("stage");
            if (hubIdNode == null || stageNode == null) {
                System.err.println("AlertProcessor: ignoring message missing hubId/stage: " + json);
                return;
            }

            int stage = stageNode.asInt();
            if (stage < ALERT_THRESHOLD) {
                return;
            }

            String hubId = hubIdNode.asText();
            String message = "\uD83D\uDEA8 Simulated post: Hub " + hubId
                    + " is experiencing a severe delay (stage " + stage + "/8).";
            Alert alert = new Alert(hubId, stage, message, Instant.now());
            alerts.add(alert);

            // "Outbound webhook, simulated social post" per the README — a log
            // line stands in for the real call this service doesn't actually make.
            System.out.println("[AlertBot] " + message);
        } catch (Exception e) {
            System.err.println("AlertProcessor: ignoring unparseable message: " + json + " (" + e.getMessage() + ")");
        }
    }

    public List<Alert> recentAlerts() {
        return Collections.unmodifiableList(alerts);
    }
}