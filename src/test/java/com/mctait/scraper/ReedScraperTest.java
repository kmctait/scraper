package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mctait.config.ScrapingProperties;
import com.mctait.model.JobPosting;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReedScraperTest {

    private static final String BASE_URL = "https://www.reed.co.uk/api/1.0/search";
    private static final String API_KEY = "test-api-key";

    private final OkHttpClient client = new OkHttpClient();
    private final ScrapingProperties properties = new ScrapingProperties();

    private ReedScraper scraper() {
        return new ReedScraper(BASE_URL, API_KEY, "/tmp", client, properties);
    }

    @Test
    void buildUrl_withKeywordsAndLocation_encodesParameters() {
        String url = scraper().buildUrl("Python Developer", "Manchester", 1);

        assertTrue(url.contains("keywords=Python+Developer"));
        assertTrue(url.contains("locationName=Manchester"));
        assertTrue(url.contains("distanceFromLocation=25"));
        assertTrue(url.contains("resultsToTake=50"));
    }

    @Test
    void buildUrl_withPageNumber_requestsThatPage() {
        String url = scraper().buildUrl("Python Developer", "Manchester", 3);

        assertTrue(url.contains("page=3"), url);
    }

    @Test
    void buildUrl_withOnlyKeywords_buildsCorrectUrl() {
        String url = scraper().buildUrl("Python", null, 1);

        assertTrue(url.contains("keywords=Python"));
        assertFalse(url.contains("locationName="));
    }

    @Test
    void mapJobPosting_parsesValidJson() {
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
        JobPosting job = scraper().mapJobPosting(jobObj);

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
        String json = """
                {
                    "jobTitle": "Developer"
                }
                """;

        JsonObject jobObj = JsonParser.parseString(json).getAsJsonObject();
        JobPosting job = scraper().mapJobPosting(jobObj);

        assertEquals("Developer", job.getTitle());
        assertEquals("", job.getCompanyName());
        assertEquals("", job.getLocation());
        assertEquals("", job.getSalary());
    }

    @Test
    void mapJobPosting_withOnlyMinimumSalary_rendersSingleBound() {
        String json = """
                {
                    "jobTitle": "Developer",
                    "minimumSalary": 35000
                }
                """;

        JobPosting job = scraper().mapJobPosting(JsonParser.parseString(json).getAsJsonObject());

        assertEquals("Min: £35,000", job.getSalary());
    }

    @Test
    void mapJobPosting_withLongDescription_truncatesTo200Chars() {
        String longDescription = "A".repeat(300);
        String json = """
                {
                    "jobTitle": "Developer",
                    "jobDescription": "%s"
                }
                """.formatted(longDescription);

        JsonObject jobObj = JsonParser.parseString(json).getAsJsonObject();
        JobPosting job = scraper().mapJobPosting(jobObj);

        assertEquals(203, job.getSummary().length());
        assertTrue(job.getSummary().endsWith("..."));
    }

    @Test
    void scrape_withBlankApiKey_failsBeforeAnyRequest() {
        ReedScraper scraper = new ReedScraper(BASE_URL, " ", "/tmp", client, properties);

        ScrapeException error = assertThrows(ScrapeException.class,
                () -> scraper.scrape("Java", "London", null));

        assertTrue(error.getMessage().contains("REED_API_KEY"), error.getMessage());
    }

    @Test
    void getSourceName_returnsReed() {
        assertEquals("Reed", scraper().getSourceName());
    }
}
