package com.wtc.worldbank.transform;

import com.wtc.worldbank.model.Observation;
import com.wtc.worldbank.model.RejectedRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObservationTransformer {

    public record Result(List<Observation> valid, List<RejectedRow> rejected) {}

    /** One raw staging row, all text. */
    public record RawObservation(String indicatorId, String countryIso3, String year, String value) {}

    public Result transform(List<RawObservation> raw, Set<String> validCountryCodes) {
        List<Observation> valid = new ArrayList<>();
        List<RejectedRow> rejected = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (RawObservation r : raw) {
            String key = r.indicatorId() + "|" + r.countryIso3() + "|" + r.year();

            if (r.countryIso3() == null || !validCountryCodes.contains(r.countryIso3())) {
                rejected.add(new RejectedRow("stg_observations", key, "not_a_country"));
                continue;
            }

            if (r.value() == null || r.value().isBlank()) {
                rejected.add(new RejectedRow("stg_observations", key, "missing_value"));
                continue;
            }

            int year;
            try {
                year = Integer.parseInt(r.year().trim());
            } catch (NumberFormatException | NullPointerException e) {
                rejected.add(new RejectedRow("stg_observations", key, "bad_year"));
                continue;
            }

            double value;
            try {
                value = Double.parseDouble(r.value().trim());
            } catch (NumberFormatException e) {
                rejected.add(new RejectedRow("stg_observations", key, "bad_value"));
                continue;
            }
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                rejected.add(new RejectedRow("stg_observations", key, "bad_value"));
                continue;
            }

            if (!seen.add(key)) {
                rejected.add(new RejectedRow("stg_observations", key, "duplicate"));
                continue;
            }

            valid.add(new Observation(r.indicatorId(), r.countryIso3(), year, value));
        }
        return new Result(valid, rejected);
    }
}