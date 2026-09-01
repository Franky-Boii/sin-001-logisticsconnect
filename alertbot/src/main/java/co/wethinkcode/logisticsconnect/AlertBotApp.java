package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqSubscriber;
import io.javalin.Javalin;

public class AlertBotApp {

    public static void main(String[] args) {
        AlertProcessor alertProcessor = new AlertProcessor();
        MqSubscriber subscriber = new MqSubscriber(alertProcessor);
        Runtime.getRuntime().addShutdownHook(new Thread(subscriber::close));

        Javalin app = Javalin.create().start(7054);

        app.get("/health", ctx -> ctx.result("OK"));

        // Posts proactive delay notifications to public transit social media pages
        // (simulated as a log line — see AlertProcessor). This endpoint exists so
        // the simulated posts can be verified from outside the process too.
        app.get("/alerts", ctx -> ctx.json(alertProcessor.recentAlerts()));
    }
}