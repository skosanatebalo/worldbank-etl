package com.wtc.worldbank.transform;

import com.wtc.worldbank.transform.ObservationTransformer.RawObservation;
import com.wtc.worldbank.transform.ObservationTransformer.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservationTransformerTest {

    private final ObservationTransformer transformer = new ObservationTransformer();

    @Test
    void splitsValidAndRejectedRows() {
        Set<String> countries = Set.of("ZAF", "USA");

        List<RawObservation> raw = List.of(
                new RawObservation("SP.POP.TOTL", "ZAF", "2023", "60000000"),   // good
                new RawObservation("SP.POP.TOTL", "WLD", "2023", "8000000000"), // aggregate
                new RawObservation("SP.POP.TOTL", "USA", "2023", null),         // missing value
                new RawObservation("SP.POP.TOTL", "USA", "abc", "1"),           // bad year
                new RawObservation("SP.POP.TOTL", "USA", "2022", "lots"),       // bad value
                new RawObservation("SP.POP.TOTL", "ZAF", "2023", "60000000"));  // duplicate

        Result result = transformer.transform(raw, countries);

        assertEquals(1, result.valid().size());
        assertEquals(5, result.rejected().size());
        assertEquals("not_a_country", result.rejected().get(0).reason());
        assertEquals("missing_value", result.rejected().get(1).reason());
        assertEquals("bad_year", result.rejected().get(2).reason());
        assertEquals("bad_value", result.rejected().get(3).reason());
        assertEquals("duplicate", result.rejected().get(4).reason());
    }
}