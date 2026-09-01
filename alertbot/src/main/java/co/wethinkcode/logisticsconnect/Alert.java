package co.wethinkcode.logisticsconnect;

import java.time.Instant;

/** One triggered alert — a simulated social post about a severely delayed hub. */
public record Alert(String hubId, int stage, String message, Instant triggeredAt) {
}