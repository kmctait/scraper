package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.mctait.config.ScrapingProperties;
import com.mctait.model.JobPosting;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class ReedScraper extends AbstractJobScraper {

    /** Reed rejects requests asking for more than 100 results per page. */
    private static final int MAX_RESULTS_PER_PAGE = 100;

    private final String apiBaseUrl;
    private final String apiKey;

    public ReedScraper(
            @Value("${reed.api.base-url:https://www.reed.co.uk/api/1.0/search}") String apiBaseUrl,
            @Value("${reed.api.key:}") String apiKey,
            @Value("${output.base-path:scrapedJobs}") String outputBasePath,
            OkHttpClient client,
            ScrapingProperties properties) {
        super(outputBasePath, client, properties);
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
    }

    @Override
    protected String buildUrl(String keywords, String location, int page) {
        StringBuilder url = new StringBuilder(apiBaseUrl);
        url.append("?resultsToTake=").append(Math.min(properties.getResultsPerPage(), MAX_RESULTS_PER_PAGE));
        url.append("&page=").append(page);

        if (keywords != null && !keywords.isEmpty()) {
            url.append("&keywords=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8));
        }

        if (location != null && !location.isEmpty()) {
            url.append("&locationName=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
            url.append("&distanceFromLocation=25");
        }

        return url.toString();
    }

    @Override
    protected void addAuthHeader(Builder requestBuilder) {
        String credentials = Credentials.basic(apiKey, "");
        requestBuilder.header("Authorization", credentials);
    }

    @Override
    protected JobPosting mapJobPosting(JsonObject jobObj) {
        String title = getStringField(jobObj, "jobTitle");
        String companyName = getStringField(jobObj, "employerName");
        String location = getStringField(jobObj, "locationName");
        String salary = getSalary(jobObj);
        String description = getStringField(jobObj, "jobDescription");
        String url = getStringField(jobObj, "jobUrl");

        return new JobPosting(
                title,
                companyName,
                location,
                salary,
                truncateSummary(description),
                url,
                getSourceName()
        );
    }

    private String getSalary(JsonObject jobObj) {
        Long minimum = getLongField(jobObj, "minimumSalary");
        Long maximum = getLongField(jobObj, "maximumSalary");
        return formatSalaryRange(minimum, maximum);
    }

    @Override
    protected void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ScrapeException(
                    "REED_API_KEY is not set; export it or set reed.api.key in your configuration");
        }
    }

    @Override
    public String getSourceName() {
        return "Reed";
    }
}
