package com.mctait;

import com.mctait.model.JobPosting;
import com.mctait.service.JobAggregatorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class Main implements CommandLineRunner {

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

        System.out.println("\n=== UK Job Scraper ===");
        System.out.println("Keywords: " + keywords);
        System.out.println("Location: " + location);
        System.out.println("Output: " + (outputLocation != null ? outputLocation : "default"));
        System.out.println("========================\n");

        List<JobPosting> jobs = aggregatorService.aggregateJobs(keywords, location, outputLocation);

        System.out.println("\n=== Results (" + jobs.size() + " jobs found) ===");

        if (jobs.isEmpty()) {
            System.out.println("No jobs found. Try different keywords or location.");
        }

        System.exit(0);
    }
}
