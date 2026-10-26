package com.wtc.worldbank.transform;

import com.wtc.worldbank.transform.CountryTransformer.RawCountry;
import com.wtc.worldbank.transform.CountryTransformer.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CountryTransformerTest {

    private final CountryTransformer transformer = new CountryTransformer();

    @Test
    void rejectsAggregatesAndDuplicatesAndCleansFields() {
        List<RawCountry> raw = List.of(
                new RawCountry("ZAF", "ZA", "South Africa", "Sub-Saharan Africa ",
                        "Upper middle income", "Pretoria", "28.1871", "-25.746"),
                new RawCountry("WLD", "1W", "World", "Aggregates", "Aggregates", "", "", ""),
                new RawCountry("XXX", "XX", "Nowhere", "Europe & Central Asia",
                        "High income", "", "", ""),
                new RawCountry("ZAF", "ZA", "South Africa", "Sub-Saharan Africa",
                        "Upper middle income", "Pretoria", "28.1871", "-25.746"));

        Result result = transformer.transform(raw);

        assertEquals(2, result.valid().size());
        assertEquals(2, result.rejected().size());

        assertEquals("aggregate", result.rejected().get(0).reason());
        assertEquals("duplicate", result.rejected().get(1).reason());

        assertEquals("Sub-Saharan Africa", result.valid().get(0).region());

        assertNull(result.valid().get(1).capital());
        assertNull(result.valid().get(1).longitude());
    }
}
