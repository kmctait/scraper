package com.mctait.scraper;

import com.mctait.model.JobPosting;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AdzunaScraper extends AbstractJobScraper {

    private final String apiBaseUrl;
    private final String appId;
    private final String appKey;

    public AdzunaScraper(
            @Value("${adzuna.api.base-url}") String apiBaseUrl,
            @Value("${adzuna.api.app-id}") String appId,
            @Value("${adzuna.api.app-key}") String appKey,
            @Value("${output.base-path}") String outputBasePath) {
        super(outputBasePath);
        this.apiBaseUrl = apiBaseUrl;
        this.appId = appId;
        this.appKey = appKey;
    }

    @Override
    protected String buildUrl(String keywords, String location) {
        StringBuilder url = new StringBuilder(apiBaseUrl);
        url.append("?app_id=").append(appId);
        url.append("&app_key=").append(appKey);
        url.append("&results_per_page=100");
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
            summary = description.length() > 200
                    ? description.substring(0, 200) + "..."
                    : description;
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
        StringBuilder salary = new StringBuilder();
        if (jobObj.has("salary_min") && !jobObj.get("salary_min").isJsonNull()) {
            salary.append("Min: £").append(formatCurrency(jobObj.get("salary_min").getAsLong()));
        }
        if (jobObj.has("salary_max") && !jobObj.get("salary_max").isJsonNull()) {
            if (!salary.isEmpty()) salary.append(" ");
            salary.append("Max: £").append(formatCurrency(jobObj.get("salary_max").getAsLong()));
        }
        if (jobObj.has("contract_time") && !jobObj.get("contract_time").isJsonNull()) {
            if (!salary.isEmpty()) salary.append(" ");
            salary.append("(").append(jobObj.get("contract_time").getAsString()).append(")");
        }
        return salary.toString();
    }

    @Override
    public String getSourceName() {
        return "Adzuna";
    }
}