package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.opencsv.CSVReader;

import co.wethinkcode.logisticsconnect.model.HubRecord;

class HubCsvCleanerTest {

    private final HubCsvCleaner cleaner = new HubCsvCleaner();

    @Nested
    @DisplayName("casing and padding")
    class CasingAndPadding {

        @Test
        @DisplayName("lowercase hub id is normalized to uppercase")
        void normalizesHubIdCasing() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"h-501", "Western Cape", "Cape Town Port", "yes"}
            ));
            assertEquals("H-501", result.get(0).hubId());
        }

        @Test
        @DisplayName("leading/trailing padding on province is trimmed")
        void trimsProvincePadding() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-500", " Gauteng ", "Johannesburg Central", "Y"}
            ));
            assertEquals("Gauteng", result.get(0).province());
        }

        @Test
        @DisplayName("internal double spaces in sorting center are collapsed")
        void collapsesDoubleSpaces() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-505", "Western Cape", "Cape Town  Port", "TRUE"}
            ));
            assertEquals("Cape Town Port", result.get(0).sortingCenter());
        }

        @Test
        @DisplayName("lowercase sorting center is normalized to title case")
        void normalizesSortingCenterCasing() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-510", "Gauteng", "johannesburg central", "FALSE"}
            ));
            assertEquals("Johannesburg Central", result.get(0).sortingCenter());
        }

        @Test
        @DisplayName("KwaZulu-Natal variants are normalized to one canonical province name")
        void normalizesKwaZuluNatalVariants() {
            List<HubRecord> result = cleaner.clean(List.of(
                    new String[]{"H-503", "KwaZulu-Natal", "Durban Harbour", "Y"},
                    new String[]{"H-506", "Kwa-Zulu Natal", "Durban Harbour", "1"},
                    new String[]{"H-516", "KwaZulu Natal", "Durban Harbour", "true"}
            ));

            assertEquals(3, result.size());
            assertEquals("KwaZulu-Natal", result.get(0).province());
            assertEquals("KwaZulu-Natal", result.get(1).province());
            assertEquals("KwaZulu-Natal", result.get(2).province());
        }
    }

    @Nested
    @DisplayName("boolean/flag normalization")
    class BooleanNormalization {

        @ParameterizedTest(name = "\"{0}\" -> true")
        @CsvSource({"Y", "yes", "YES", "true", "TRUE", "1"})
        void normalizesTruthyVariants(String rawActive) {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-900", "Gauteng", "Test Hub", rawActive}
            ));
            assertEquals(Boolean.TRUE, result.get(0).active());
        }

        @ParameterizedTest(name = "\"{0}\" -> false")
        @CsvSource({"N", "no", "FALSE", "false", "0"})
        void normalizesFalsyVariants(String rawActive) {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-901", "Gauteng", "Test Hub", rawActive}
            ));
            assertEquals(Boolean.FALSE, result.get(0).active());
        }
    }

    @Nested
    @DisplayName("missing / placeholder / invalid values")
    class MissingAndInvalidValues {

        @Test
        void unknownActiveBecomesNull() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-511", "Limpopo", "Polokwane Hub", "unknown"}
            ));
            assertNull(result.get(0).active());
        }

        @Test
        void naActiveBecomesNullAndFlagged() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-517", "Northern Cape", "Kimberley Hub", "N/A"}
            ));
            HubRecord record = result.get(0);
            assertNull(record.active());
            assertFalse(record.notes().isEmpty());
        }

        @Test
        void blankProvinceIsFlaggedNotDropped() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-508", "", "Pretoria North", "yes"}
            ));
            assertEquals(1, result.size());
            assertFalse(result.get(0).notes().isEmpty());
        }

        @Test
        void garbageActiveValueDoesNotThrow() {
            assertDoesNotThrow(() -> {
                List<HubRecord> result = cleaner.clean(List.<String[]>of(
                        new String[]{"H-902", "Gauteng", "Test Hub", "banana"}
                ));
                assertNull(result.get(0).active());
            });
        }
    }

    @Nested
    @DisplayName("duplicate detection")
    class DuplicateDetection {

        @Test
        void collapsesExactDuplicateWithDifferentIdCasing() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-500", "Gauteng", "Johannesburg Central", "Y"},
                    new String[]{"h-500", "Gauteng", "Johannesburg Central", "Y"}
            ));
            assertEquals(1, result.size());
        }

        @Test
        void conflictingDuplicateIsResolvedAndFlagged() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-500", "Gauteng", "Johannesburg Central", "Y"},
                    new String[]{"h-500", "Gauteng", "Johannesburg Central", "N"}
            ));
            long h500Count = result.stream()
                    .filter(r -> "H-500".equals(r.hubId()))
                    .count();

            assertEquals(1, h500Count);
            assertFalse(result.get(0).notes().isEmpty());
        }

        @Test
        void doesNotMergeDistinctHubIds() {
            List<HubRecord> result = cleaner.clean(List.<String[]>of(
                    new String[]{"H-500", "Gauteng", "Johannesburg Central", "Y"},
                    new String[]{"H-504", "Gauteng", "Johannesburg Central", "true"}
            ));
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("end-to-end against the real file")
    class RealFile {

        @Test
        void cleansRealFileWithoutThrowing() throws Exception {
            List<String[]> rawRows = readRawRows("/hubs-global.csv");
            List<HubRecord> result = assertDoesNotThrow(() -> cleaner.clean(rawRows));

            assertFalse(result.isEmpty());
            assertTrue(result.size() <= rawRows.size());
            assertTrue(result.stream()
                    .allMatch(r -> r.hubId() != null && !r.hubId().isBlank()));
        }

        private List<String[]> readRawRows(String classpathResource) throws Exception {
            try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
                assertNotNull(
                        in,
                        "expected " + classpathResource + " on the test classpath"
                );

                Reader reader = new InputStreamReader(
                        in,
                        StandardCharsets.UTF_8
                );

                try (CSVReader csvReader = new CSVReader(reader)) {
                    List<String[]> all = csvReader.readAll();
                    return all.subList(1, all.size());
                }
            }
        }
    }
}

