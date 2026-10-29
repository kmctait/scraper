package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mctait.config.ScrapingProperties;
import com.mctait.model.JobPosting;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdzunaScraperTest {

    private static final String BASE_URL = "https://api.adzuna.com/v1/api/jobs/gb/search";
    private static final String APP_ID = "test-app-id";
    private static final String APP_KEY = "test-app-key";

    private final OkHttpClient client = new OkHttpClient();
    private final ScrapingProperties properties = new ScrapingProperties();

    private AdzunaScraper scraper() {
        return scraper(BASE_URL);
    }

    private AdzunaScraper scraper(String baseUrl) {
        return new AdzunaScraper(baseUrl, APP_ID, APP_KEY, "/tmp", client, properties);
    }

    @Test
    void buildUrl_withKeywordsAndLocation_encodesParameters() {
        String url = scraper().buildUrl("Java Developer", "London", 1);

        assertTrue(url.startsWith(BASE_URL + "/1?"), url);
        assertTrue(url.contains("app_id=" + APP_ID));
        assertTrue(url.contains("app_key=" + APP_KEY));
        assertTrue(url.contains("what=Java+Developer"));
        assertTrue(url.contains("where=London"));
        assertTrue(url.contains("results_per_page=50"));
    }

    @Test
    void buildUrl_forLaterPage_appendsPageNumberToPath() {
        String url = scraper().buildUrl("Java Developer", "London", 2);

        assertTrue(url.startsWith(BASE_URL + "/2?"), url);
    }

    @Test
    void buildUrl_whenBaseUrlAlreadyHasPage_replacesIt() {
        String url = scraper(BASE_URL + "/1").buildUrl("Java Developer", "London", 3);

        assertTrue(url.startsWith(BASE_URL + "/3?"), url);
    }

    @Test
    void buildUrl_withOversizedResultsPerPage_capsAtAdzunaLimit() {
        properties.setResultsPerPage(500);

        String url = scraper().buildUrl("Java", "London", 1);

        assertTrue(url.contains("results_per_page=50"));
    }

    @Test
    void buildUrl_withNullKeywords_handlesGracefully() {
        String url = scraper().buildUrl(null, "London", 1);

        assertTrue(url.contains("app_id=" + APP_ID));
        assertFalse(url.contains("what="));
        assertTrue(url.contains("where=London"));
    }

    @Test
    void buildUrl_withEmptyKeywords_handlesGracefully() {
        String url = scraper().buildUrl("", "London", 1);

        assertTrue(url.contains("app_id=" + APP_ID));
        assertFalse(url.contains("what="));
        assertTrue(url.contains("where=London"));
    }

    @Test
    void mapJobPosting_parsesValidJson() {
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
        JobPosting job = scraper().mapJobPosting(jobObj);

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
        String json = """
                {
                    "title": "Developer"
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
    void mapJobPosting_withLongDescription_truncatesTo200Chars() {
        String json = """
                {
                    "title": "Developer",
                    "description": "%s"
                }
                """.formatted("A".repeat(300));

        JobPosting job = scraper().mapJobPosting(JsonParser.parseString(json).getAsJsonObject());

        assertEquals(203, job.getSummary().length());
        assertTrue(job.getSummary().endsWith("..."));
    }

    @Test
    void mapJobPosting_formatsContractTimeAndMarksPredictedSalary() {
        String json = """
                {
                    "title": "Data Engineer",
                    "company": {"display_name": "Data Co"},
                    "location": {"display_name": "Leeds"},
                    "salary_min": 40000,
                    "salary_max": 55000,
                    "contract_time": "full_time",
                    "salary_is_predicted": 1,
                    "description": "Build pipelines",
                    "redirect_url": "https://example.com/job/2",
                    "created": "2026-10-01T10:00:00Z"
                }
                """;

        JobPosting job = scraper().mapJobPosting(JsonParser.parseString(json).getAsJsonObject());

        assertEquals("Min: £40,000 Max: £55,000 (Full time, estimated)", job.getSalary());
    }

    @Test
    void mapJobPosting_whenSalaryIsNotPredicted_addsNoEstimatedTag() {
        String json = """
                {
                    "title": "Data Engineer",
                    "salary_min": 40000,
                    "salary_is_predicted": 0
                }
                """;

        JobPosting job = scraper().mapJobPosting(JsonParser.parseString(json).getAsJsonObject());

        assertEquals("Min: £40,000", job.getSalary());
    }

    @Test
    void mapJobPosting_whenSalaryIsPredictedButMissing_addsNoTag() {
        String json = """
                {
                    "title": "Data Engineer",
                    "salary_is_predicted": true
                }
                """;

        JobPosting job = scraper().mapJobPosting(JsonParser.parseString(json).getAsJsonObject());

        assertEquals("", job.getSalary());
    }

    @Test
    void scrape_withBlankCredentials_failsBeforeAnyRequest() {
        AdzunaScraper scraper = new AdzunaScraper(BASE_URL, "", " ", "/tmp", client, properties);

        ScrapeException error = assertThrows(ScrapeException.class,
                () -> scraper.scrape("Java", "London", null));

        assertTrue(error.getMessage().contains("ADZUNA_APP_ID"), error.getMessage());
    }

    @Test
    void getSourceName_returnsAdzuna() {
        assertEquals("Adzuna", scraper().getSourceName());
    }
}
