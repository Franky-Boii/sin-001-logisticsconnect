package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.model.HubDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HubCache is the pure in-memory query layer hub-service serves from.
 */
class HubCacheTest {

    private final List<HubDto> sample = List.of(
            new HubDto("H-500", "Gauteng", "Johannesburg Central", true, List.of()),
            new HubDto("H-501", "Western Cape", "Cape Town Port", true, List.of()),
            new HubDto("H-508", "", "Pretoria North", true, List.of("missing province for hub H-508")),
            new HubDto("H-511", "Limpopo", "Polokwane Hub", null, List.of("unrecognized active value"))
    );

    @Nested
    @DisplayName("all()")
    class All {
        @Test
        @DisplayName("returns every hub the cache was built with, in order")
        void returnsAllHubs() {
            HubCache cache = new HubCache(sample);
            assertEquals(sample, cache.all());
        }

        @Test
        @DisplayName("an empty source list produces an empty result")
        void handlesEmptySource() {
            HubCache cache = new HubCache(List.of());
            assertNotNull(cache.all());
            assertTrue(cache.all().isEmpty());
        }
    }

    @Nested
    @DisplayName("byId()")
    class ById {
        @Test
        @DisplayName("finds a hub by exact id")
        void findsExactMatch() {
            HubCache cache = new HubCache(sample);
            Optional<HubDto> result = cache.byId("H-501");
            assertTrue(result.isPresent());
            assertEquals("Cape Town Port", result.get().sortingCenter());
        }

        @Test
        @DisplayName("lookup is case-insensitive")
        void lookupIsCaseInsensitive() {
            HubCache cache = new HubCache(sample);
            assertTrue(cache.byId("h-501").isPresent());
        }

        @Test
        @DisplayName("an unknown id returns empty")
        void unknownIdReturnsEmpty() {
            HubCache cache = new HubCache(sample);
            assertTrue(cache.byId("H-999").isEmpty());
        }
    }

    @Nested
    @DisplayName("provinces()")
    class Provinces {
        @Test
        @DisplayName("returns distinct non-blank provinces sorted")
        void returnsDistinctSortedProvinces() {
            HubCache cache = new HubCache(sample);
            assertEquals(List.of("Gauteng", "Limpopo", "Western Cape"), cache.provinces());
        }

        @Test
        @DisplayName("excludes blank provinces")
        void excludesBlankProvince() {
            HubCache cache = new HubCache(sample);
            assertFalse(cache.provinces().contains(""));
        }

        @Test
        @DisplayName("deduplicates repeated provinces")
        void deduplicatesRepeatedProvince() {
            List<HubDto> twoInGauteng = List.of(
                    new HubDto("H-500", "Gauteng", "Johannesburg Central", true, List.of()),
                    new HubDto("H-504", "Gauteng", "Midrand Hub", true, List.of())
            );
            HubCache cache = new HubCache(twoInGauteng);
            assertEquals(List.of("Gauteng"), cache.provinces());
        }
    }
}
