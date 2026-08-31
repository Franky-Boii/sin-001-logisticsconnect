package co.wethinkcode.logisticsconnect.mq;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

import co.wethinkcode.logisticsconnect.StageCache;

/**
 * Subscribes to {@link MqConfig#TOPIC} and hands each message's raw body to
 * {@link StageCache}. This class owns the only JMS-specific code on the consumer
 * side — the actual caching/lookup logic lives in StageCache, which is unit tested
 * without any of this.
 */
public class MqSubscriber implements MessageListener, AutoCloseable {

    private final Connection connection;
    private final StageCache stageCache;

    public MqSubscriber(StageCache stageCache) {
        this.stageCache = stageCache;
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            throw new IllegalStateException(
                    "failed to subscribe to ActiveMQ topic " + MqConfig.TOPIC
                            + " at " + MqConfig.BROKER_URL
                            + " — is `docker compose up -d` running in common/?", e);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                stageCache.recordFromJson(textMessage.getText());
            }
        } catch (JMSException e) {
            System.err.println("MqSubscriber: failed to read message body: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (JMSException e) {
            
        }
    }
}