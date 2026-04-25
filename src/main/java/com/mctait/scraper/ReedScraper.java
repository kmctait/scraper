package com.mctait.scraper;

import com.mctait.model.JobPosting;
import com.google.gson.JsonObject;
import okhttp3.Credentials;
import okhttp3.Request.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class ReedScraper extends AbstractJobScraper {

    private final String apiBaseUrl;
    private final String apiKey;

    public ReedScraper(
            @Value("${reed.api.base-url}") String apiBaseUrl,
            @Value("${reed.api.key}") String apiKey,
            @Value("${output.base-path}") String outputBasePath) {
        super(outputBasePath);
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
    }

    protected String buildUrl(String keywords, String location) {
        StringBuilder url = new StringBuilder(apiBaseUrl);

        if (keywords != null && !keywords.isEmpty()) {
            url.append("?keywords=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8));
        }

        if (location != null && !location.isEmpty()) {
            url.append(keywords != null && !keywords.isEmpty() ? "&" : "?");
            url.append("locationName=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
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

        String summary = description;
        if (description != null && description.length() > 200) {
            summary = description.substring(0, 200) + "...";
        }

        return new JobPosting(
                title,
                companyName,
                location,
                salary,
                summary,
                url,
                getSourceName()
        );
    }

    private String getSalary(JsonObject jobObj) {
        StringBuilder salary = new StringBuilder();
        if (jobObj.has("minimumSalary") && !jobObj.get("minimumSalary").isJsonNull()) {
            salary.append("Min: £").append(formatCurrency(jobObj.get("minimumSalary").getAsLong()));
        }
        if (jobObj.has("maximumSalary") && !jobObj.get("maximumSalary").isJsonNull()) {
            if (!salary.isEmpty()) salary.append(" ");
            salary.append("Max: £").append(formatCurrency(jobObj.get("maximumSalary").getAsLong()));
        }
        return salary.toString();
    }

    @Override
    public String getSourceName() {
        return "Reed";
    }
}