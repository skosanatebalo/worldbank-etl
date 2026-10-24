package com.wtc.worldbank.transform;

import com.wtc.worldbank.model.Country;
import com.wtc.worldbank.model.RejectedRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CountryTransformer {

    public record Result(List<Country> valid, List<RejectedRow> rejected) {}

    public record RawCountry(String id, String iso2, String name, String region,
                             String incomeLevel, String capital, String longitude, String latitude) {}

    public Result transform(List<RawCountry> raw) {
        List<Country> valid = new ArrayList<>();
        List<RejectedRow> rejected = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (RawCountry r : raw) {
            String id = trimToNull(r.id());
            if (id == null) {
                rejected.add(new RejectedRow("stg_countries", String.valueOf(r.name()), "missing_id"));
                continue;
            }

            String region = trimToNull(r.region());
            if (region == null) {
                rejected.add(new RejectedRow("stg_countries", id, "missing_region"));
                continue;
            }
            if (region.equals("Aggregates")) {
                rejected.add(new RejectedRow("stg_countries", id, "aggregate"));
                continue;
            }

            if (!seen.add(id)) {
                rejected.add(new RejectedRow("stg_countries", id, "duplicate"));
                continue;
            }

            valid.add(new Country(
                    id,
                    trimToNull(r.iso2()),
                    trimToNull(r.name()),
                    region,
                    trimToNull(r.incomeLevel()),
                    trimToNull(r.capital()),
                    parseDoubleOrNull(r.longitude()),
                    parseDoubleOrNull(r.latitude())));
        }
        return new Result(valid, rejected);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Double parseDoubleOrNull(String s) {
        String t = trimToNull(s);
        if (t == null) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}