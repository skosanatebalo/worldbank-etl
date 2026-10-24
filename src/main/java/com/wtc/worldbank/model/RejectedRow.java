// model/RejectedRow.java
package com.wtc.worldbank.model;

public record RejectedRow(String sourceTable, String key, String reason) {}