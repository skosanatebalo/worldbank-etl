package com.wtc.worldbank;

import com.wtc.worldbank.config.Database;
import com.wtc.worldbank.extract.IndicatorExtractor;
import com.wtc.worldbank.extract.WorldBankClient;
import com.wtc.worldbank.load.RunTracker;
import com.wtc.worldbank.load.StagingLoader;
import com.wtc.worldbank.load.StagingSchema;
import com.wtc.worldbank.load.WarehouseLoader;
import com.wtc.worldbank.load.WarehouseSchema;
import com.wtc.worldbank.transform.TransformRunner;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.logging.Logger;

public class Pipeline {

    private static final Logger LOG = Logger.getLogger(Pipeline.class.getName());
    private static final String[] INDICATORS = {"NY.GDP.MKTP.CD", "SP.POP.TOTL", "SL.UEM.TOTL.ZS"};

    public void run() throws Exception {
        RunTracker tracker = new RunTracker();

        // A separate connection for tracking, so the run is recorded
        // even when the main work rolls back.
        try (Connection tracking = Database.getConnection()) {
            RunTracker.createTable(tracking);
            long runId = tracker.start(tracking);
            LOG.info("Run " + runId + " started");

            try {
                Path rawDir = Path.of("raw");

                LOG.info("Extracting from the World Bank API");
                IndicatorExtractor extractor = new IndicatorExtractor(new WorldBankClient(), rawDir);
                for (String indicator : INDICATORS) {
                    extractor.extract(indicator, 2015, 2023);
                }

                try (Connection work = Database.getConnection()) {
                    LOG.info("Loading staging tables");
                    StagingSchema.create(work);
                    StagingLoader loader = new StagingLoader();
                    loader.loadObservations(work, rawDir);
                    loader.loadCountries(work, rawDir.resolve("countries.json"));

                    LOG.info("Transforming and validating");
                    TransformRunner.Output out = new TransformRunner().run(work);

                    LOG.info("Loading final tables");
                    WarehouseSchema.create(work);
                    new WarehouseLoader().load(work, out.countries(), out.observations());

                    tracker.success(tracking, runId,
                            out.countries().size(), out.observations().size(), out.rejectedCount());
                    LOG.info("Run " + runId + " finished: " + out.countries().size() + " countries, "
                            + out.observations().size() + " observations, "
                            + out.rejectedCount() + " rejected");
                }
            } catch (Exception e) {
                LOG.severe("Run " + runId + " failed: " + e);
                tracker.failure(tracking, runId, e.toString());
                throw e;
            }
        }
    }
}