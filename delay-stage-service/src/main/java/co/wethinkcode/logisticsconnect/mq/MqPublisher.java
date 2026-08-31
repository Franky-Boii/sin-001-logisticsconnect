package co.wethinkcode.logisticsconnect.mq;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes a JSON message ({@code {"hubId": ..., "stage": ...}}) to
 * {@link MqConfig#TOPIC} whenever DelayStageServiceApp's POST endpoint accepts a
 * stage change. One connection/session/producer is opened at startup and reused —
 * see close() for shutdown.
 */
public class MqPublisher implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqPublisher() {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            producer = session.createProducer(topic);
        } catch (JMSException e) {
            throw new IllegalStateException(
                    "failed to connect to ActiveMQ broker at " + MqConfig.BROKER_URL
                            + " — is `docker compose up -d` running in common/?", e);
        }
    }

    /**
     * Publishes a stage change. Callers should treat a failure here as a warning,
     * not a reason to fail the underlying REST request — the whole point of moving
     * this off the request path is that a broker hiccup shouldn't break the
     * synchronous contract of POST /delay-stage/{hubId}.
     */
    public void publishStageChange(String hubId, int stage) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("hubId", hubId, "stage", stage));
            TextMessage message = session.createTextMessage(json);
            producer.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("failed to publish stage change for hub " + hubId, e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (JMSException e) {
            // best-effort shutdown
        }
    }
}