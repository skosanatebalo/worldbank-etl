// model/Observation.java
package com.wtc.worldbank.model;

public record Observation(String indicatorId, String countryIso3, int year, double value) {}