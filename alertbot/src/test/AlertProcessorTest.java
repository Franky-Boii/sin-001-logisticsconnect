package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AlertProcessorTest {

    @Nested
    @DisplayName("threshold behavior")
    class Threshold {

        @Test
        @DisplayName("a stage below the threshold does not trigger an alert")
        void belowThresholdDoesNotAlert() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":5}");
            assertTrue(processor.recentAlerts().isEmpty());
        }

        @Test
        @DisplayName("a stage at the threshold triggers an alert")
        void atThresholdTriggersAlert() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":" + AlertProcessor.ALERT_THRESHOLD + "}");
            assertEquals(1, processor.recentAlerts().size());
        }

        @Test
        @DisplayName("a stage above the threshold triggers an alert")
        void aboveThresholdTriggersAlert() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":8}");
            assertEquals(1, processor.recentAlerts().size());
        }

        @Test
        @DisplayName("the recorded alert names the hub and the stage that triggered it")
        void recordedAlertContainsHubAndStage() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":7}");

            Alert alert = processor.recentAlerts().get(0);
            assertEquals("H-500", alert.hubId());
            assertEquals(7, alert.stage());
            assertTrue(alert.message().contains("H-500"), "simulated post should mention the hub");
        }
    }

    @Nested
    @DisplayName("multiple messages")
    class MultipleMessages {

        @Test
        @DisplayName("each qualifying message adds its own alert, in order")
        void accumulatesAlertsInOrder() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":6}");
            processor.processMessage("{\"hubId\":\"H-501\",\"stage\":8}");

            List<Alert> alerts = processor.recentAlerts();
            assertEquals(2, alerts.size());
            assertEquals("H-500", alerts.get(0).hubId());
            assertEquals("H-501", alerts.get(1).hubId());
        }

        @Test
        @DisplayName("a below-threshold message between two qualifying ones is skipped, not recorded as a gap")
        void skipsNonQualifyingMessages() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":8}");
            processor.processMessage("{\"hubId\":\"H-501\",\"stage\":2}");
            processor.processMessage("{\"hubId\":\"H-502\",\"stage\":7}");

            assertEquals(2, processor.recentAlerts().size());
        }
    }

    @Nested
    @DisplayName("malformed input")
    class MalformedInput {

        @Test
        @DisplayName("malformed JSON is ignored, not thrown")
        void malformedJsonDoesNotThrow() {
            AlertProcessor processor = new AlertProcessor();
            assertDoesNotThrow(() -> processor.processMessage("not json"));
            assertTrue(processor.recentAlerts().isEmpty());
        }

        @Test
        @DisplayName("a message missing stage is ignored, not thrown")
        void missingStageDoesNotThrow() {
            AlertProcessor processor = new AlertProcessor();
            assertDoesNotThrow(() -> processor.processMessage("{\"hubId\":\"H-500\"}"));
            assertTrue(processor.recentAlerts().isEmpty());
        }

        @Test
        @DisplayName("a malformed message doesn't corrupt previously recorded alerts")
        void malformedMessageDoesNotCorruptExistingAlerts() {
            AlertProcessor processor = new AlertProcessor();
            processor.processMessage("{\"hubId\":\"H-500\",\"stage\":8}");
            processor.processMessage("garbage");
            assertEquals(1, processor.recentAlerts().size());
        }
    }
}