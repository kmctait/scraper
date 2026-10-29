package com.mctait.scraper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mctait.config.ScrapingProperties;
import com.mctait.model.JobPosting;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractJobScraper implements JobScraper {

    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final int MAX_FILENAME_COMPONENT_LENGTH = 60;
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final String outputBasePath;
    protected final OkHttpClient client;
    protected final ScrapingProperties properties;

    protected AbstractJobScraper(String outputBasePath, OkHttpClient client, ScrapingProperties properties) {
        this.outputBasePath = outputBasePath;
        this.client = client;
        this.properties = properties;
    }

    /**
     * Builds the request URL for a given (1-based) page of results.
     */
    protected abstract String buildUrl(String keywords, String location, int page);

    /**
     * Fails fast when required credentials are missing, before any network request is made.
     * Subclasses override this to report the environment variables the user needs to set.
     *
     * @throws ScrapeException when the scraper cannot run with the current configuration
     */
    protected void validateConfiguration() {
    }

    /**
     * Maps a single API result entry onto the application's model.
     */
    protected abstract JobPosting mapJobPosting(JsonObject jobObj);

    @Override
    public List<JobPosting> scrape(String keywords, String location, String outputLocation) {
        validateConfiguration();
        List<JobPosting> jobs = new ArrayList<>();

        for (int page = 1; page <= properties.getMaxPages(); page++) {
            log.debug("Fetching {} page {} for '{}' in '{}'", getSourceName(), page, keywords, location);
            List<JobPosting> pageJobs = fetchJobs(buildUrl(keywords, location, page));
            jobs.addAll(pageJobs);

            if (pageJobs.size() < properties.getResultsPerPage()) {
                break;
            }
        }

        writeToFile(jobs, keywords, location, outputLocation);
        return jobs;
    }

    protected List<JobPosting> fetchJobs(String url) {
        return parseJobsJson(fetchJson(url));
    }

    /**
     * Performs the HTTP request, retrying transient failures with exponential backoff.
     *
     * @throws ScrapeException if the request fails permanently or the response body is unusable
     */
    protected String fetchJson(String url) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/json");

        addAuthHeader(requestBuilder);

        Request request = requestBuilder.build();
        int attempts = Math.max(1, properties.getMaxRetries() + 1);
        IOException lastIoFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new ScrapeException(getSourceName() + " returned an empty response body");
                    }
                    return body.string();
                }

                if (!RETRYABLE_STATUS_CODES.contains(response.code())) {
                    throw new ScrapeException(
                            "%s API request failed with HTTP %d".formatted(getSourceName(), response.code()));
                }

                log.warn("{} API request failed with HTTP {} (attempt {}/{})",
                        getSourceName(), response.code(), attempt, attempts);
            } catch (IOException e) {
                lastIoFailure = e;
                log.warn("{} API request failed: {} (attempt {}/{})",
                        getSourceName(), e.getMessage(), attempt, attempts);
            }

            if (attempt < attempts && !sleepBeforeRetry(attempt)) {
                throw new ScrapeException(getSourceName() + " request interrupted during retry backoff");
            }
        }

        throw new ScrapeException(
                "%s API request failed after %d attempt(s)".formatted(getSourceName(), attempts), lastIoFailure);
    }

    private boolean sleepBeforeRetry(int attempt) {
        long backoffMillis = properties.getRetryBackoff().toMillis() * (1L << (attempt - 1));
        if (backoffMillis <= 0) {
            return true;
        }
        try {
            Thread.sleep(backoffMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    protected void addAuthHeader(Request.Builder requestBuilder) {
    }

    protected List<JobPosting> parseJobsJson(String json) {
        List<JobPosting> jobs = new ArrayList<>();

        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new ScrapeException(getSourceName() + " returned malformed JSON", e);
        }

        JsonArray resultsArray = root.getAsJsonArray("results");
        if (resultsArray == null) {
            log.warn("No results array in {} response", getSourceName());
            return jobs;
        }

        for (int i = 0; i < resultsArray.size(); i++) {
            try {
                JobPosting job = mapJobPosting(resultsArray.get(i).getAsJsonObject());
                if (job != null) {
                    jobs.add(job);
                } else {
                    log.warn("Mapper returned no job for {} result {}", getSourceName(), i);
                }
            } catch (RuntimeException e) {
                log.warn("Skipping unparsable {} job entry {}", getSourceName(), i, e);
            }
        }

        return jobs;
    }

    protected void writeToFile(List<JobPosting> jobs, String keywords, String location, String outputLocation) {
        Path file = resolveOutputFile(keywords, location, outputLocation);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, buildHtml(jobs, keywords, location), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ScrapeException(
                    "Failed to write %s output to %s".formatted(getSourceName(), file), e);
        }

        log.info("Wrote HTML to {}", file.toAbsolutePath());
    }

    /**
     * Resolves the output file, sanitising user-supplied values and refusing any path that
     * would escape the configured output directory.
     */
    protected Path resolveOutputFile(String keywords, String location, String outputLocation) {
        String configuredBasePath = (outputLocation != null && !outputLocation.isBlank())
                ? outputLocation
                : outputBasePath;

        if (configuredBasePath == null || configuredBasePath.isBlank()) {
            throw new ScrapeException("No output directory configured for " + getSourceName());
        }

        Path baseDir;
        try {
            baseDir = Paths.get(configuredBasePath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new ScrapeException("Invalid output directory: " + configuredBasePath, e);
        }

        String sourceName = getSourceName();
        Path sourceDir = baseDir.resolve(sanitizeFileNameComponent(sourceName, "source")).normalize();
        if (!sourceDir.startsWith(baseDir)) {
            throw new ScrapeException("Output directory escapes the configured base path: " + sourceDir);
        }

        String fileName = "%s_%s_%s.html".formatted(
                sanitizeFileNameComponent(sourceName.toLowerCase(Locale.ROOT), "source"),
                sanitizeFileNameComponent(keywords, "keywords"),
                sanitizeFileNameComponent(location, "location"));

        Path file = sourceDir.resolve(fileName).normalize();
        if (!file.startsWith(sourceDir) || !file.startsWith(baseDir)) {
            throw new ScrapeException("Refusing to write outside the output directory: " + file);
        }

        return file;
    }

    /**
     * Reduces an arbitrary value to a safe single path segment (no separators, no dot segments).
     */
    protected String sanitizeFileNameComponent(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String sanitized = value.trim()
                .replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[._]+", "")
                .replaceAll("[._]+$", "");

        if (sanitized.length() > MAX_FILENAME_COMPONENT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_FILENAME_COMPONENT_LENGTH)
                    .replaceAll("[._]+$", "");
        }

        if (sanitized.isEmpty()) {
            return fallback;
        }
        return sanitized;
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
            String url = safeUrl(job.getUrl());
            if (url == null) {
                html.append("<span class=\"job-title\">").append(escapeHtml(job.getTitle())).append("</span>\n");
            } else {
                html.append("<a class=\"job-title\" href=\"").append(escapeHtml(url)).append("\">")
                        .append(escapeHtml(job.getTitle())).append("</a>\n");
            }
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
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Returns the URL only when it is an absolute {@code http}/{@code https} URL, so API data
     * cannot inject {@code javascript:} or {@code data:} links into the generated report.
     */
    protected String safeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && uri.getHost() != null) {
                return trimmed;
            }
        } catch (URISyntaxException e) {
            log.debug("Ignoring job URL that is not valid: {}", trimmed);
        }
        return null;
    }

    protected String getStringField(JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            return obj.get(field).getAsString();
        }
        return "";
    }

    protected String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    protected String truncateSummary(String description) {
        return truncate(description, MAX_SUMMARY_LENGTH);
    }

    protected String formatCurrency(long amount) {
        return String.format(Locale.UK, "%,d", amount);
    }

    /**
     * Renders a "Min: ... Max: ..." salary range, omitting missing bounds.
     */
    protected String formatSalaryRange(Long minimum, Long maximum) {
        StringBuilder salary = new StringBuilder();
        if (minimum != null) {
            salary.append("Min: £").append(formatCurrency(minimum));
        }
        if (maximum != null) {
            if (!salary.isEmpty()) salary.append(" ");
            salary.append("Max: £").append(formatCurrency(maximum));
        }
        return salary.toString();
    }

    /**
     * Appends a page number to a base URL, tolerating a base URL that already ends in a page
     * number (for example {@code .../search/1}).
     */
    protected String pageUrl(String baseUrl, int page) {
        String normalized = baseUrl.replaceFirst("/\\d+/?$", "");
        return normalized + "/" + page;
    }
}
