package com.mctait.scraper;

import com.mctait.model.JobPosting;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class AdzunaScraper implements JobScraper {

    private final String apiBaseUrl;
    private final String appId;
    private final String appKey;
    private final String outputBasePath;

    private final OkHttpClient client;

    public AdzunaScraper(
            @Value("${adzuna.api.base-url}") String apiBaseUrl,
            @Value("${adzuna.api.app-id}") String appId,
            @Value("${adzuna.api.app-key}") String appKey,
            @Value("${output.base-path}") String outputBasePath) {
        this.apiBaseUrl = apiBaseUrl;
        this.appId = appId;
        this.appKey = appKey;
        this.outputBasePath = outputBasePath;
        this.client = new OkHttpClient();
    }

    @Override
    public List<JobPosting> scrape(String keywords, String location, String outputLocation) {
        List<JobPosting> jobs = new ArrayList<>();

        try {
            String url = buildUrl(keywords, location);
            jobs = fetchJobs(url);
            writeToFile(jobs, keywords, location, outputLocation);
        } catch (Exception e) {
            System.err.println("Error fetching from Adzuna: " + e.getMessage());
        }

        return jobs;
    }

    private void writeToFile(List<JobPosting> jobs, String keywords, String location, String outputLocation) {
        try {
            String basePath = (outputLocation != null && !outputLocation.isEmpty()) 
                ? outputLocation 
                : outputBasePath;
            String fileName = "adzuna_" + keywords.replace(" ", "_") + "_" + location.replace(" ", "_") + ".html";
            File outputDir = new File(basePath, "Adzuna");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File file = new File(outputDir, fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(buildHtml(jobs, keywords, location));
            }
            System.out.println("Wrote HTML to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing Adzuna HTML file: " + e.getMessage());
        }
    }

    private String buildHtml(List<JobPosting> jobs, String keywords, String location) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>Adzuna Jobs - ").append(escapeHtml(keywords)).append(" - ").append(escapeHtml(location)).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; background: #f5f5f5; }\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #1292b9; padding-bottom: 10px; }\n");
        html.append(".job-card { background: white; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append(".job-title { font-size: 18px; font-weight: 600; color: #1292b9; text-decoration: none; }\n");
        html.append(".job-title:hover { text-decoration: underline; }\n");
        html.append(".company { color: #666; margin: 4px 0; }\n");
        html.append(".location { color: #888; font-size: 14px; }\n");
        html.append(".salary { color: #2e7d32; font-weight: 500; }\n");
        html.append(".summary { color: #555; margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee; }\n");
        html.append(".stats { color: #666; margin-bottom: 20px; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<h1>Adzuna Jobs</h1>\n");
        html.append("<p class=\"stats\">").append(escapeHtml(keywords)).append(" in ").append(escapeHtml(location)).append(" - ").append(jobs.size()).append(" jobs</p>\n");
        for (JobPosting job : jobs) {
            html.append("<div class=\"job-card\">\n");
            html.append("<a class=\"job-title\" href=\"").append(escapeHtml(job.getUrl())).append("\">").append(escapeHtml(job.getTitle())).append("</a>\n");
            html.append("<div class=\"company\">").append(escapeHtml(job.getCompanyName())).append("</div>\n");
            html.append("<div class=\"location\">").append(escapeHtml(job.getLocation())).append("</div>\n");
            if (job.getSalary() != null && !job.getSalary().isEmpty()) {
                html.append("<div class=\"salary\">").append(escapeHtml(job.getSalary())).append("</div>\n");
            }
            html.append("<div class=\"summary\">").append(escapeHtml(job.getSummary())).append("</div>\n");
            html.append("</div>\n");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String buildUrl(String keywords, String location) {
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

    private List<JobPosting> fetchJobs(String url) throws IOException {
        List<JobPosting> jobs = new ArrayList<>();

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.err.println("Adzuna API request failed: " + response.code());
                return jobs;
            }

            String json = response.body().string();
            jobs = parseJobsJson(json);
        }

        return jobs;
    }

    private List<JobPosting> parseJobsJson(String json) {
        List<JobPosting> jobs = new ArrayList<>();

        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
            com.google.gson.JsonArray resultsArray = root.getAsJsonArray("results");

            if (resultsArray == null) {
                return jobs;
            }

            for (int i = 0; i < resultsArray.size(); i++) {
                try {
                    com.google.gson.JsonObject jobObj = resultsArray.get(i).getAsJsonObject();

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

                    JobPosting job = new JobPosting(
                            title,
                            companyName,
                            location,
                            salary,
                            summary,
                            url,
                            "Adzuna"
                    );
                    jobs.add(job);
                } catch (Exception e) {
                    System.err.println("Error parsing Adzuna job entry: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Adzuna JSON: " + e.getMessage());
        }

        return jobs;
    }

    private String getStringField(com.google.gson.JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            return obj.get(field).getAsString();
        }
        return "";
    }

    private String getCompanyName(com.google.gson.JsonObject jobObj) {
        if (jobObj.has("company") && !jobObj.get("company").isJsonNull()) {
            com.google.gson.JsonObject companyObj = jobObj.getAsJsonObject("company");
            if (companyObj.has("display_name") && !companyObj.get("display_name").isJsonNull()) {
                return companyObj.get("display_name").getAsString();
            }
        }
        return "";
    }

    private String getLocation(com.google.gson.JsonObject jobObj) {
        if (jobObj.has("location") && !jobObj.get("location").isJsonNull()) {
            com.google.gson.JsonObject locationObj = jobObj.getAsJsonObject("location");
            if (locationObj.has("display_name") && !locationObj.get("display_name").isJsonNull()) {
                return locationObj.get("display_name").getAsString();
            }
        }
        return "";
    }

    private String getSalary(com.google.gson.JsonObject jobObj) {
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

    private String formatCurrency(long amount) {
        return String.format("%,d", amount);
    }

    @Override
    public String getSourceName() {
        return "Adzuna";
    }
}