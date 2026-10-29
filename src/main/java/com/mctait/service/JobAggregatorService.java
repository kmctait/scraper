package com.mctait.service;

import com.mctait.model.JobPosting;
import com.mctait.scraper.JobScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(JobAggregatorService.class);

    private final List<JobScraper> scrapers;

    public JobAggregatorService(List<JobScraper> scrapers) {
        this.scrapers = scrapers;
    }

    /**
     * Runs every scraper, isolating failures so one broken source does not abort the others.
     * Failures are reported back through {@link AggregationResult#failedSources()}.
     */
    public AggregationResult aggregateJobs(String keywords, String location, String outputLocation) {
        List<JobPosting> allJobs = new ArrayList<>();
        List<String> failedSources = new ArrayList<>();

        for (JobScraper scraper : scrapers) {
            String sourceName = scraper.getSourceName();
            log.info("Scraping from {}...", sourceName);
            try {
                List<JobPosting> jobs = scraper.scrape(keywords, location, outputLocation);
                allJobs.addAll(jobs);
                log.info("Found {} jobs from {}", jobs.size(), sourceName);
            } catch (RuntimeException e) {
                log.error("Scraping from {} failed", sourceName, e);
                failedSources.add(sourceName);
            }
        }

        if (!failedSources.isEmpty()) {
            log.error("Scraper(s) failed: {}", String.join(", ", failedSources));
        }

        return new AggregationResult(allJobs, failedSources);
    }
}
