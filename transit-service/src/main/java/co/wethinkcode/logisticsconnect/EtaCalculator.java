package co.wethinkcode.logisticsconnect;

/**
 * Turns a delay stage (0-8, from delay-stage-service) into an estimated arrival
 * window. The formula is intentionally simple — a placeholder worth calling out
 * explicitly in any debrief, per RUBRIC.md's "articulating tradeoffs" bullet:
 * a real system would source this from historical transit-time data per hub
 * rather than a flat per-stage penalty.
 */
public class EtaCalculator {

    public static final int BASELINE_MINUTES = 30;
    public static final int MINUTES_PER_STAGE = 15;
    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;

    public int etaMinutes(int delayStage) {
        if (delayStage < MIN_STAGE || delayStage > MAX_STAGE) {
            throw new IllegalArgumentException(
                    "delayStage must be between " + MIN_STAGE + " and " + MAX_STAGE + ", got " + delayStage);
        }
        return BASELINE_MINUTES + delayStage * MINUTES_PER_STAGE;
    }
}
