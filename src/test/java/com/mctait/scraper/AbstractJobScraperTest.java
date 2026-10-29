package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.mctait.config.ScrapingProperties;
import com.mctait.model.JobPosting;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AbstractJobScraperTest {

    @TempDir
    Path tempDir;

    @Test
    void scrape_stopsAfterFirstShortPage() {
        ScrapingProperties properties = properties(5, 2);
        StubScraper scraper = new StubScraper(tempDir.toString(), properties, List.of(2, 1));

        List<JobPosting> jobs = scraper.scrape("Java", "London", null);

        assertEquals(List.of("page-1", "page-2"), scraper.requestedUrls);
        assertEquals(3, jobs.size());
    }

    @Test
    void scrape_neverRequestsMoreThanMaxPages() {
        ScrapingProperties properties = properties(2, 2);
        StubScraper scraper = new StubScraper(tempDir.toString(), properties, List.of(2, 2, 2, 2));

        List<JobPosting> jobs = scraper.scrape("Java", "London", null);

        assertEquals(List.of("page-1", "page-2"), scraper.requestedUrls);
        assertEquals(4, jobs.size());
    }

    @Test
    void scrape_writesHtmlInsideTheOutputDirectory() throws IOException {
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of(1));

        scraper.scrape("Java Developer", "London", null);

        Path expected = tempDir.resolve("Stub").resolve("stub_Java_Developer_London.html");
        assertTrue(Files.exists(expected), "expected " + expected + " to exist");
        assertTrue(Files.readString(expected).contains("Job 0"));
    }

    @Test
    void scrape_sanitizesPathTraversalInKeywordsAndLocation() throws IOException {
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of(1));

        scraper.scrape("../../etc/passwd", "..", null);

        Path sourceDir = tempDir.resolve("Stub");
        try (Stream<Path> files = Files.list(sourceDir)) {
            List<Path> written = new ArrayList<>(files.toList());
            assertEquals(1, written.size());
            assertEquals("stub_etc_passwd_location.html", written.get(0).getFileName().toString());
            assertTrue(written.get(0).normalize().startsWith(tempDir));
        }
    }

    @Test
    void scrape_usesOutputLocationOverride() {
        Path overrideDir = tempDir.resolve("override");
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of(0));

        scraper.scrape("Java", "London", overrideDir.toString());

        assertTrue(Files.exists(overrideDir.resolve("Stub").resolve("stub_Java_London.html")));
    }

    @Test
    void buildHtml_escapesJobFields() {
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of());
        JobPosting job = new JobPosting("<script>", "A & B", "\"quoted\"",
                "£1", "<b>summary</b>", "https://example.com?a=1&b=2", "Stub");

        String html = scraper.buildHtml(List.of(job), "<Java>", "London");

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("A &amp; B"));
        assertTrue(html.contains("&quot;quoted&quot;"));
        assertTrue(html.contains("https://example.com?a=1&amp;b=2"));
        assertTrue(html.contains("&lt;Java&gt;"));
    }

    @Test
    void parseJobsJson_skipsNullMappings() {
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of());

        List<JobPosting> jobs = scraper.parseJobsJson("""
                {"results":[{"title":"A"},{"title":"B"}]}
                """);

        assertEquals(2, jobs.size());
    }

    @Test
    void parseJobsJson_withMalformedJson_throwsScrapeException() {
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of());

        assertThrows(ScrapeException.class, () -> scraper.parseJobsJson("not json"));
    }

    @Test
    void parseJobsJson_withoutResultsArray_returnsEmptyList() {
        StubScraper scraper = new StubScraper(tempDir.toString(), properties(3, 50), List.of());

        assertTrue(scraper.parseJobsJson("{}").isEmpty());
    }

    private static ScrapingProperties properties(int maxPages, int resultsPerPage) {
        ScrapingProperties properties = new ScrapingProperties();
        properties.setMaxPages(maxPages);
        properties.setResultsPerPage(resultsPerPage);
        return properties;
    }

    /**
     * Scraper that serves in-memory pages instead of hitting the network.
     */
    private static final class StubScraper extends AbstractJobScraper {

        private final List<Integer> pageSizes;
        private final List<String> requestedUrls = new ArrayList<>();

        private StubScraper(String outputBasePath, ScrapingProperties properties, List<Integer> pageSizes) {
            super(outputBasePath, new OkHttpClient(), properties);
            this.pageSizes = pageSizes;
        }

        @Override
        protected String buildUrl(String keywords, String location, int page) {
            return "page-" + page;
        }

        @Override
        protected List<JobPosting> fetchJobs(String url) {
            requestedUrls.add(url);
            int index = requestedUrls.size() - 1;
            int size = index < pageSizes.size() ? pageSizes.get(index) : 0;
            return IntStream.range(0, size)
                    .mapToObj(i -> new JobPosting("Job " + i, "Company", "London", "",
                            "Summary", "https://example.com/" + i, getSourceName()))
                    .toList();
        }

        @Override
        protected JobPosting mapJobPosting(JsonObject jobObj) {
            return new JobPosting(getStringField(jobObj, "title"), "", "", "", "",
                    "https://example.com", getSourceName());
        }

        @Override
        public String getSourceName() {
            return "Stub";
        }
    }
}
