package com.mctait.service;

import com.mctait.model.JobPosting;
import com.mctait.scraper.AdzunaScraper;
import com.mctait.scraper.ReedScraper;
import com.mctait.scraper.ScrapeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobAggregatorServiceTest {

    private static final JobPosting ADZUNA_JOB =
            new JobPosting("Java Dev", "Company A", "London", "£50k", "Summary A", "http://url1", "Adzuna");
    private static final JobPosting REED_JOB =
            new JobPosting("Python Dev", "Company B", "Manchester", "£40k", "Summary B", "http://url2", "Reed");

    @Mock
    private AdzunaScraper adzunaScraper;

    @Mock
    private ReedScraper reedScraper;

    private JobAggregatorService service;

    @BeforeEach
    void setUp() {
        service = new JobAggregatorService(List.of(adzunaScraper, reedScraper));
        lenient().when(adzunaScraper.getSourceName()).thenReturn("Adzuna");
        lenient().when(reedScraper.getSourceName()).thenReturn("Reed");
    }

    @Test
    void aggregateJobs_combinesResultsFromAllScrapers() {
        when(adzunaScraper.scrape("Java", "London", null)).thenReturn(List.of(ADZUNA_JOB));
        when(reedScraper.scrape("Java", "London", null)).thenReturn(List.of(REED_JOB));

        AggregationResult result = service.aggregateJobs("Java", "London", null);

        assertEquals(List.of(ADZUNA_JOB, REED_JOB), result.jobs());
        assertFalse(result.hasFailures());
        verify(adzunaScraper).scrape("Java", "London", null);
        verify(reedScraper).scrape("Java", "London", null);
    }

    @Test
    void aggregateJobs_withEmptyResults_returnsEmptyListAndNoFailures() {
        when(adzunaScraper.scrape("Java", "London", null)).thenReturn(List.of());
        when(reedScraper.scrape("Java", "London", null)).thenReturn(List.of());

        AggregationResult result = service.aggregateJobs("Java", "London", null);

        assertTrue(result.jobs().isEmpty());
        assertFalse(result.hasFailures());
    }

    @Test
    void aggregateJobs_passesOutputLocationToScrapers() {
        when(adzunaScraper.scrape(eq("Java"), eq("London"), eq("/custom/path"))).thenReturn(List.of(ADZUNA_JOB));
        when(reedScraper.scrape(eq("Java"), eq("London"), eq("/custom/path"))).thenReturn(List.of());

        service.aggregateJobs("Java", "London", "/custom/path");

        verify(adzunaScraper).scrape("Java", "London", "/custom/path");
        verify(reedScraper).scrape("Java", "London", "/custom/path");
    }

    @Test
    void aggregateJobs_whenOneSourceFails_keepsOtherResultsAndReportsFailure() {
        when(adzunaScraper.scrape("Java", "London", null)).thenReturn(List.of(ADZUNA_JOB));
        when(reedScraper.scrape("Java", "London", null)).thenThrow(new ScrapeException("Reed API down"));

        AggregationResult result = service.aggregateJobs("Java", "London", null);

        assertEquals(List.of(ADZUNA_JOB), result.jobs());
        assertEquals(List.of("Reed"), result.failedSources());
        assertTrue(result.hasFailures());
    }

    @Test
    void aggregateJobs_whenAllSourcesFail_reportsEveryFailure() {
        when(adzunaScraper.scrape("Java", "London", null)).thenThrow(new ScrapeException("Adzuna down"));
        when(reedScraper.scrape("Java", "London", null)).thenThrow(new ScrapeException("Reed down"));

        AggregationResult result = service.aggregateJobs("Java", "London", null);

        assertTrue(result.jobs().isEmpty());
        assertEquals(List.of("Adzuna", "Reed"), result.failedSources());
    }
}
