package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mctait.model.JobPosting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdzunaScraperTest {

    @Test
    void buildUrl_withKeywordsAndLocation_encodesParameters() {
        AdzunaScraper scraper = new AdzunaScraper(
                "http://api.adzuna.com/v1/api/jobs/gb/search/1",
                "test-app-id",
                "test-app-key",
                "/tmp"
        );

        String url = scraper.buildUrl("Java Developer", "London");

        assertTrue(url.contains("app_id=test-app-id"));
        assertTrue(url.contains("app_key=test-app-key"));
        assertTrue(url.contains("what=Java+Developer"));
        assertTrue(url.contains("where=London"));
        assertTrue(url.contains("results_per_page=100"));
    }

    @Test
    void buildUrl_withNullKeywords_handlesGracefully() {
        AdzunaScraper scraper = new AdzunaScraper(
                "http://api.adzuna.com/v1/api/jobs/gb/search/1",
                "test-app-id",
                "test-app-key",
                "/tmp"
        );

        String url = scraper.buildUrl(null, "London");

        assertTrue(url.contains("app_id=test-app-id"));
        assertFalse(url.contains("what="));
        assertTrue(url.contains("where=London"));
    }

    @Test
    void buildUrl_withEmptyKeywords_handlesGracefully() {
        AdzunaScraper scraper = new AdzunaScraper(
                "http://api.adzuna.com/v1/api/jobs/gb/search/1",
                "test-app-id",
                "test-app-key",
                "/tmp"
        );

        String url = scraper.buildUrl("", "London");

        assertTrue(url.contains("app_id=test-app-id"));
        assertFalse(url.contains("what="));
        assertTrue(url.contains("where=London"));
    }

    @Test
    void mapJobPosting_parsesValidJson() {
        AdzunaScraper scraper = new AdzunaScraper(
                "http://api.adzuna.com/v1/api/jobs/gb/search/1",
                "test-app-id",
                "test-app-key",
                "/tmp"
        );

        String json = """
                {
                    "title": "Java Developer",
                    "company": {"display_name": "Tech Corp"},
                    "location": {"display_name": "London"},
                    "salary_min": 40000,
                    "salary_max": 60000,
                    "contract_time": "Full Time",
                    "description": "Great opportunity for a Java developer",
                    "redirect_url": "https://example.com/job/1",
                    "created": "2024-01-15T10:00:00Z"
                }
                """;

        JsonObject jobObj = JsonParser.parseString(json).getAsJsonObject();
        JobPosting job = scraper.mapJobPosting(jobObj);

        assertEquals("Java Developer", job.getTitle());
        assertEquals("Tech Corp", job.getCompanyName());
        assertEquals("London", job.getLocation());
        assertEquals("Min: £40,000 Max: £60,000 (Full Time)", job.getSalary());
        assertEquals("Great opportunity for a Java developer", job.getSummary());
        assertEquals("https://example.com/job/1", job.getUrl());
        assertEquals("Adzuna", job.getSourceSite());
    }

    @Test
    void mapJobPosting_withMissingFields_handlesGracefully() {
        AdzunaScraper scraper = new AdzunaScraper(
                "http://api.adzuna.com/v1/api/jobs/gb/search/1",
                "test-app-id",
                "test-app-key",
                "/tmp"
        );

        String json = """
                {
                    "title": "Developer"
                }
                """;

        JsonObject jobObj = JsonParser.parseString(json).getAsJsonObject();
        JobPosting job = scraper.mapJobPosting(jobObj);

        assertEquals("Developer", job.getTitle());
        assertEquals("", job.getCompanyName());
        assertEquals("", job.getLocation());
        assertEquals("", job.getSalary());
    }

    @Test
    void getSourceName_returnsAdzuna() {
        AdzunaScraper scraper = new AdzunaScraper(
                "http://api.adzuna.com/v1/api/jobs/gb/search/1",
                "test-app-id",
                "test-app-key",
                "/tmp"
        );

        assertEquals("Adzuna", scraper.getSourceName());
    }
}