// model/Country.java
package com.wtc.worldbank.model;

public record Country(String iso3, String iso2, String name, String region,
                      String incomeLevel, String capital, Double longitude, Double latitude) {}