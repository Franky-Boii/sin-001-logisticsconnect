package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EtaCalculatorTest {

    private final EtaCalculator calculator = new EtaCalculator();

    @Test
    @DisplayName("stage 0 (no delay) returns the baseline ETA")
    void stageZeroReturnsBaseline() {
        assertEquals(EtaCalculator.BASELINE_MINUTES, calculator.etaMinutes(0));
    }

    @ParameterizedTest(name = "stage {0} adds {0} * per-stage delay to the baseline")
    @CsvSource({"1", "3", "8"})
    @DisplayName("each stage above 0 adds a fixed delay on top of the baseline")
    void higherStageIncreasesEta(int stage) {
        int expected = EtaCalculator.BASELINE_MINUTES + stage * EtaCalculator.MINUTES_PER_STAGE;
        assertEquals(expected, calculator.etaMinutes(stage));
    }

    @Test
    @DisplayName("a negative stage is rejected")
    void rejectsNegativeStage() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> calculator.etaMinutes(-1));
    }

    @Test
    @DisplayName("a stage above the known max (8) is rejected")
    void rejectsStageAboveMax() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> calculator.etaMinutes(9));
    }
}
