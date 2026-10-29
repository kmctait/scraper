package com.mctait.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tunables shared by every scraper: pagination, per-source throttling and retry behaviour.
 */
@ConfigurationProperties(prefix = "scraping")
public class ScrapingProperties {

    /** Maximum number of result pages fetched per source. */
    private int maxPages = 3;

    /** Page size requested from each source (Adzuna caps this at 50). */
    private int resultsPerPage = 50;

    /** Number of extra attempts made after the first failed request. */
    private int maxRetries = 2;

    /** Delay before the first retry; doubles on each subsequent attempt. */
    private Duration retryBackoff = Duration.ofMillis(500);

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }
}
