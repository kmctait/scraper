package com.mctait.scraper;

import com.mctait.model.JobPosting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJobScraper implements JobScraper {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final String outputBasePath;
    protected final OkHttpClient client;

    protected AbstractJobScraper(@Value("${output.base-path}") String outputBasePath) {
        this.outputBasePath = outputBasePath;
        this.client = new OkHttpClient();
    }

    protected abstract String buildUrl(String keywords, String location);

    @Override
    public List<JobPosting> scrape(String keywords, String location, String outputLocation) {
        List<JobPosting> jobs = new ArrayList<>();

        try {
            String url = buildUrl(keywords, location);
            jobs = fetchJobs(url);
            writeToFile(jobs, keywords, location, outputLocation);
        } catch (Exception e) {
            log.error("Error fetching from {}: {}", getSourceName(), e.getMessage());
        }

        return jobs;
    }

    protected List<JobPosting> fetchJobs(String url) throws IOException {
        List<JobPosting> jobs = new ArrayList<>();

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/json");

        addAuthHeader(requestBuilder);

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("{} API request failed: {}", getSourceName(), response.code());
                return jobs;
            }

            String json = response.body().string();
            jobs = parseJobsJson(json);
        }

        return jobs;
    }

    protected void addAuthHeader(Request.Builder requestBuilder) {
    }

    protected List<JobPosting> parseJobsJson(String json) {
        List<JobPosting> jobs = new ArrayList<>();

        try {
            JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            JsonArray resultsArray = root.getAsJsonArray("results");

            if (resultsArray == null) {
                log.warn("No results array in {} response", getSourceName());
                return jobs;
            }

            for (int i = 0; i < resultsArray.size(); i++) {
                try {
                    JsonObject jobObj = resultsArray.get(i).getAsJsonObject();
                    JobPosting job = mapJobPosting(jobObj);
                    jobs.add(job);
                } catch (Exception e) {
                    log.error("Error parsing {} job entry: {}", getSourceName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing {} JSON: {}", getSourceName(), e.getMessage());
        }

        return jobs;
    }

    protected JobPosting mapJobPosting(JsonObject jobObj) {
        return null;
    }

    protected void writeToFile(List<JobPosting> jobs, String keywords, String location, String outputLocation) {
        try {
            String basePath = (outputLocation != null && !outputLocation.isEmpty())
                    ? outputLocation
                    : outputBasePath;
            String fileName = getSourceName().toLowerCase() + "_" + keywords.replace(" ", "_") + "_" + location.replace(" ", "_") + ".html";
            File outputDir = new File(basePath, getSourceName());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File file = new File(outputDir, fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(buildHtml(jobs, keywords, location));
            }
            log.info("Wrote HTML to {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing {} HTML file: {}", getSourceName(), e.getMessage());
        }
    }

    protected String buildHtml(List<JobPosting> jobs, String keywords, String location) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html><head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>").append(getSourceName()).append(" Jobs - ").append(escapeHtml(keywords)).append(" - ").append(escapeHtml(location)).append("</title>\n");
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
        html.append("<h1>").append(getSourceName()).append(" Jobs</h1>\n");
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

    protected String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    protected String getStringField(JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            return obj.get(field).getAsString();
        }
        return "";
    }

    protected String formatCurrency(long amount) {
        return String.format("%,d", amount);
    }
}