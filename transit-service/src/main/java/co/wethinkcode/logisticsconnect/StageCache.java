package co.wethinkcode.logisticsconnect;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Holds the latest delay stage seen per hub, as broadcast on
 * {@code package-status-topic}. Deliberately free of any JMS type — MqSubscriber
 * owns the broker connection and hands this class raw message bodies, which is what
 * makes this class testable without a running ActiveMQ broker (see StageCacheTest).
 */
public class StageCache {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Integer> latestStageByHubId = new ConcurrentHashMap<>();

    /**
     * Parses a {@code {"hubId": ..., "stage": ...}} message body and records it.
     * A malformed or incomplete message is logged and ignored rather than thrown —
     * one bad message on the topic shouldn't take the subscriber down.
     */
    public void recordFromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode hubIdNode = node.get("hubId");
            JsonNode stageNode = node.get("stage");
            if (hubIdNode == null || stageNode == null) {
                System.err.println("StageCache: ignoring message missing hubId/stage: " + json);
                return;
            }
            String hubId = normalize(hubIdNode.asText());
            if (!hubId.isBlank()) {
                latestStageByHubId.put(hubId, stageNode.asInt());
            }
        } catch (Exception e) {
            System.err.println("StageCache: ignoring unparseable message: " + json + " (" + e.getMessage() + ")");
        }
    }

    public int stageFor(String hubId, int defaultStage) {
        return latestStageByHubId.getOrDefault(normalize(hubId), defaultStage);
    }

    private String normalize(String hubId) {
        return hubId == null ? "" : hubId.trim().toUpperCase();
    }
}