package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mctait.model.JobPosting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReedScraperTest {

    @Test
    void buildUrl_withKeywordsAndLocation_encodesParameters() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        String url = scraper.buildUrl("Python Developer", "Manchester");

        assertTrue(url.contains("keywords=Python+Developer"));
        assertTrue(url.contains("locationName=Manchester"));
        assertTrue(url.contains("distanceFromLocation=25"));
    }

    @Test
    void buildUrl_withOnlyKeywords_buildsCorrectUrl() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        String url = scraper.buildUrl("Python", null);

        assertTrue(url.contains("keywords=Python"));
        assertFalse(url.contains("locationName="));
    }

    @Test
    void buildUrl_withNullLocation_handlesGracefully() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        String url = scraper.buildUrl("Python", null);

        assertTrue(url.contains("keywords=Python"));
        assertFalse(url.contains("locationName="));
    }

    @Test
    void mapJobPosting_parsesValidJson() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        String json = """
                {
                    "jobTitle": "Python Developer",
                    "employerName": "Tech Corp",
                    "locationName": "Manchester",
                    "minimumSalary": 35000,
                    "maximumSalary": 50000,
                    "jobDescription": "Great opportunity for a Python developer",
                    "jobUrl": "https://example.com/job/1"
                }
                """;

        JsonObject jobObj = JsonParser.parseString(json).getAsJsonObject();
        JobPosting job = scraper.mapJobPosting(jobObj);

        assertEquals("Python Developer", job.getTitle());
        assertEquals("Tech Corp", job.getCompanyName());
        assertEquals("Manchester", job.getLocation());
        assertEquals("Min: £35,000 Max: £50,000", job.getSalary());
        assertEquals("Great opportunity for a Python developer", job.getSummary());
        assertEquals("https://example.com/job/1", job.getUrl());
        assertEquals("Reed", job.getSourceSite());
    }

    @Test
    void mapJobPosting_withMissingFields_handlesGracefully() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        String json = """
                {
                    "jobTitle": "Developer"
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
    void mapJobPosting_withLongDescription_truncatesTo200Chars() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        String longDescription = "A".repeat(300);
        String json = """
                {
                    "jobTitle": "Developer",
                    "jobDescription": "%s"
                }
                """.formatted(longDescription);

        JsonObject jobObj = JsonParser.parseString(json).getAsJsonObject();
        JobPosting job = scraper.mapJobPosting(jobObj);

        assertEquals(203, job.getSummary().length());
        assertTrue(job.getSummary().endsWith("..."));
    }

    @Test
    void getSourceName_returnsReed() {
        ReedScraper scraper = new ReedScraper(
                "https://www.reed.co.uk/api/1.0/search",
                "test-api-key",
                "/tmp"
        );

        assertEquals("Reed", scraper.getSourceName());
    }
}