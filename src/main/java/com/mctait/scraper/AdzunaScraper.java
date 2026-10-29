package com.mctait.scraper;

import com.google.gson.JsonObject;
import com.mctait.config.ScrapingProperties;
import com.mctait.model.JobPosting;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AdzunaScraper extends AbstractJobScraper {

    /** Adzuna silently caps this value at 50 results per page. */
    private static final int MAX_RESULTS_PER_PAGE = 50;

    private final String apiBaseUrl;
    private final String appId;
    private final String appKey;

    public AdzunaScraper(
            @Value("${adzuna.api.base-url:https://api.adzuna.com/v1/api/jobs/gb/search}") String apiBaseUrl,
            @Value("${adzuna.api.app-id:}") String appId,
            @Value("${adzuna.api.app-key:}") String appKey,
            @Value("${output.base-path:scrapedJobs}") String outputBasePath,
            OkHttpClient client,
            ScrapingProperties properties) {
        super(outputBasePath, client, properties);
        this.apiBaseUrl = apiBaseUrl;
        this.appId = appId;
        this.appKey = appKey;
    }

    @Override
    protected String buildUrl(String keywords, String location, int page) {
        StringBuilder url = new StringBuilder(pageUrl(apiBaseUrl, page));
        url.append("?app_id=").append(URLEncoder.encode(appId, StandardCharsets.UTF_8));
        url.append("&app_key=").append(URLEncoder.encode(appKey, StandardCharsets.UTF_8));
        url.append("&results_per_page=").append(Math.min(properties.getResultsPerPage(), MAX_RESULTS_PER_PAGE));
        url.append("&content-type=application/json");

        if (keywords != null && !keywords.isEmpty()) {
            url.append("&what=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8));
        }

        if (location != null && !location.isEmpty()) {
            url.append("&where=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
        }

        return url.toString();
    }

    @Override
    protected JobPosting mapJobPosting(JsonObject jobObj) {
        String title = getStringField(jobObj, "title");
        String companyName = getCompanyName(jobObj);
        String location = getLocation(jobObj);
        String salary = getSalary(jobObj);
        String description = getStringField(jobObj, "description");
        String url = getStringField(jobObj, "redirect_url");
        String created = getStringField(jobObj, "created");

        String summary = "Posted: " + created;
        if (description != null && !description.isEmpty()) {
            summary = truncateSummary(description);
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

    private String getCompanyName(JsonObject jobObj) {
        if (jobObj.has("company") && !jobObj.get("company").isJsonNull()) {
            JsonObject companyObj = jobObj.getAsJsonObject("company");
            if (companyObj.has("display_name") && !companyObj.get("display_name").isJsonNull()) {
                return companyObj.get("display_name").getAsString();
            }
        }
        return "";
    }

    private String getLocation(JsonObject jobObj) {
        if (jobObj.has("location") && !jobObj.get("location").isJsonNull()) {
            JsonObject locationObj = jobObj.getAsJsonObject("location");
            if (locationObj.has("display_name") && !locationObj.get("display_name").isJsonNull()) {
                return locationObj.get("display_name").getAsString();
            }
        }
        return "";
    }

    private String getSalary(JsonObject jobObj) {
        Long minimum = getLongField(jobObj, "salary_min");
        Long maximum = getLongField(jobObj, "salary_max");
        String salary = formatSalaryRange(minimum, maximum);

        if (jobObj.has("contract_time") && !jobObj.get("contract_time").isJsonNull()) {
            StringBuilder withContract = new StringBuilder(salary);
            if (!withContract.isEmpty()) withContract.append(" ");
            withContract.append("(").append(jobObj.get("contract_time").getAsString()).append(")");
            return withContract.toString();
        }

        return salary;
    }

    private Long getLongField(JsonObject jobObj, String field) {
        if (jobObj.has(field) && !jobObj.get(field).isJsonNull()) {
            return jobObj.get(field).getAsLong();
        }
        return null;
    }

    @Override
    public String getSourceName() {
        return "Adzuna";
    }
}
