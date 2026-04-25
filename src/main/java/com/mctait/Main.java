package com.mctait;

import com.mctait.model.JobPosting;
import com.mctait.service.JobAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class Main implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private final JobAggregatorService aggregatorService;

    public Main(JobAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) {
        String keywords = "Java Developer";
        String location = "London";
        String outputLocation = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--keywords=")) {
                keywords = arg.substring("--keywords=".length());
            } else if (arg.startsWith("--location=")) {
                location = arg.substring("--location=".length());
            } else if (arg.startsWith("--output-location=")) {
                outputLocation = arg.substring("--output-location=".length());
            } else if ("--keywords".equals(arg) && i + 1 < args.length) {
                keywords = args[++i];
            } else if ("--location".equals(arg) && i + 1 < args.length) {
                location = args[++i];
            } else if ("--output-location".equals(arg) && i + 1 < args.length) {
                outputLocation = args[++i];
            }
        }

        log.info("\n=== UK Job Scraper ===");
        log.info("Keywords: {}", keywords);
        log.info("Location: {}", location);
        log.info("Output: {}", outputLocation != null ? outputLocation : "default");
        log.info("========================\n");

        List<JobPosting> jobs = aggregatorService.aggregateJobs(keywords, location, outputLocation);

        log.info("\n=== Results ({} jobs found) ===", jobs.size());

        if (jobs.isEmpty()) {
            log.warn("No jobs found. Try different keywords or location.");
        }
        System.exit(0);
    }
}
