package com.wtc.worldbank.extract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IndicatorExtractor {

    private static final String BASE = "https://api.worldbank.org/v2";

    private final WorldBankClient client;
    private final Path rawDir;
    private final ObjectMapper mapper = new ObjectMapper();

    public IndicatorExtractor(WorldBankClient client, Path rawDir) {
        this.client = client;
        this.rawDir = rawDir;
    }

    /** Downloads every page for one indicator; returns the number of pages saved. */
    public int extract(String indicator, int startYear, int endYear)
            throws IOException, InterruptedException {
        Files.createDirectories(rawDir);

        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            String url = BASE + "/country/all/indicator/" + indicator
                    + "?format=json&per_page=1000&date=" + startYear + ":" + endYear
                    + "&page=" + page;

            String body = client.get(url);

            JsonNode pagesNode = mapper.readTree(body).get(0).get("pages");
            if (pagesNode == null) {
                throw new IOException("Unexpected response for " + url + ": " + body);
            }
            totalPages = pagesNode.asInt();

            Files.writeString(rawDir.resolve(indicator + "_page" + page + ".json"), body);
            System.out.println(indicator + ": saved page " + page + " of " + totalPages);
            page++;
        }
        return totalPages;
    }
}