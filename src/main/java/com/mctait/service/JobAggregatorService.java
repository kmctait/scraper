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

    public List<JobPosting> aggregateJobs(String keywords, String location, String outputLocation) {
        List<JobPosting> allJobs = new ArrayList<>();

        for (JobScraper scraper : scrapers) {
            log.info("Scraping from {}...", scraper.getSourceName());
            List<JobPosting> jobs = scraper.scrape(keywords, location, outputLocation);
            allJobs.addAll(jobs);
            log.info("Found {} jobs from {}", jobs.size(), scraper.getSourceName());
        }

        return allJobs;
    }
}
