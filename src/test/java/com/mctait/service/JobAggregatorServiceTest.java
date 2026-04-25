package com.mctait.service;

import com.mctait.model.JobPosting;
import com.mctait.scraper.AdzunaScraper;
import com.mctait.scraper.ReedScraper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobAggregatorServiceTest {

    @Mock
    private AdzunaScraper adzunaScraper;

    @Mock
    private ReedScraper reedScraper;

    private JobAggregatorService service;

    @BeforeEach
    void setUp() {
        service = new JobAggregatorService(adzunaScraper, reedScraper);
    }

    @Test
    void aggregateJobs_combinesResultsFromAllScrapers() {
        when(adzunaScraper.getSourceName()).thenReturn("Adzuna");
        when(reedScraper.getSourceName()).thenReturn("Reed");

        List<JobPosting> adzunaJobs = List.of(
                new JobPosting("Java Dev", "Company A", "London", "£50k", "Summary A", "http://url1", "Adzuna")
        );
        List<JobPosting> reedJobs = List.of(
                new JobPosting("Python Dev", "Company B", "Manchester", "£40k", "Summary B", "http://url2", "Reed")
        );

        when(adzunaScraper.scrape("Java", "London", null)).thenReturn(adzunaJobs);
        when(reedScraper.scrape("Java", "London", null)).thenReturn(reedJobs);

        List<JobPosting> result = service.aggregateJobs("Java", "London", null);

        assertEquals(2, result.size());
        verify(adzunaScraper).scrape("Java", "London", null);
        verify(reedScraper).scrape("Java", "London", null);
    }

    @Test
    void aggregateJobs_withEmptyResults_returnsEmptyList() {
        when(adzunaScraper.getSourceName()).thenReturn("Adzuna");
        when(reedScraper.getSourceName()).thenReturn("Reed");

        when(adzunaScraper.scrape("Java", "London", null)).thenReturn(List.of());
        when(reedScraper.scrape("Java", "London", null)).thenReturn(List.of());

        List<JobPosting> result = service.aggregateJobs("Java", "London", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void aggregateJobs_passesOutputLocationToScrapers() {
        List<JobPosting> jobs = List.of(
                new JobPosting("Java Dev", "Company A", "London", "£50k", "Summary A", "http://url1", "Adzuna")
        );
        when(adzunaScraper.getSourceName()).thenReturn("Adzuna");
        when(reedScraper.getSourceName()).thenReturn("Reed");

        when(adzunaScraper.scrape(eq("Java"), eq("London"), eq("/custom/path"))).thenReturn(jobs);
        when(reedScraper.scrape(eq("Java"), eq("London"), eq("/custom/path"))).thenReturn(List.of());

        service.aggregateJobs("Java", "London", "/custom/path");

        verify(adzunaScraper).scrape("Java", "London", "/custom/path");
        verify(reedScraper).scrape("Java", "London", "/custom/path");
    }

    @Test
    void aggregateJobs_callsBothScrapers() {
        when(adzunaScraper.getSourceName()).thenReturn("Adzuna");
        when(reedScraper.getSourceName()).thenReturn("Reed");

        when(adzunaScraper.scrape(any(), any(), any())).thenReturn(List.of());
        when(reedScraper.scrape(any(), any(), any())).thenReturn(List.of());

        service.aggregateJobs("Java", "London", null);

        verify(adzunaScraper).scrape("Java", "London", null);
        verify(reedScraper).scrape("Java", "London", null);
    }
}