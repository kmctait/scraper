package com.mctait.cli;

import com.mctait.service.AggregationResult;
import com.mctait.service.JobAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runs a single scrape from the command line and decides the process exit code.
 *
 * <p>Excluded from the {@code test} profile so that booting the application context in tests
 * never performs live API calls or writes files.
 */
@Component
@Profile("!test")
public class JobSearchRunner implements ApplicationRunner, ExitCodeGenerator {

    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_SCRAPE_FAILED = 1;
    public static final int EXIT_USAGE_ERROR = 2;

    private static final Logger log = LoggerFactory.getLogger(JobSearchRunner.class);

    private final JobAggregatorService aggregatorService;

    private volatile int exitCode = EXIT_SUCCESS;

    public JobSearchRunner(JobAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        CliArguments cliArguments;
        try {
            cliArguments = CliArguments.parse(args.getSourceArgs());
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            log.info(CliArguments.usage());
            exitCode = EXIT_USAGE_ERROR;
            return;
        }

        if (cliArguments.help()) {
            log.info(CliArguments.usage());
            return;
        }

        log.info("\n=== UK Job Scraper ===");
        log.info("Keywords: {}", cliArguments.keywords());
        log.info("Location: {}", cliArguments.location());
        log.info("Output: {}", cliArguments.outputLocation() != null ? cliArguments.outputLocation() : "default");
        log.info("========================\n");

        AggregationResult result = aggregatorService.aggregateJobs(
                cliArguments.keywords(), cliArguments.location(), cliArguments.outputLocation());

        log.info("\n=== Results ({} jobs found) ===", result.jobs().size());

        if (result.jobs().isEmpty() && !result.hasFailures()) {
            log.warn("No jobs found. Try different keywords or location.");
        }

        exitCode = result.hasFailures() ? EXIT_SCRAPE_FAILED : EXIT_SUCCESS;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
