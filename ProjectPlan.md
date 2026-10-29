# Implementation Plan - UK Job Scraper

## Iteration 1 - Foundation (done)

### 1. Project Setup
- [x] Upgrade Java version from 17 to **21** in `build.gradle`
- [x] Add dependencies: `OkHttp` (HTTP client), `Gson` (JSON parsing)
- [x] Drop the unused `spring-boot-starter-web` dependency: the application is a CLI, so it runs
      with `WebApplicationType.NONE` and no embedded web server
- [x] Drop the unused `jsoup` dependency

### 2. Core Domain Model
- [x] `JobPosting.java` in `src/main/java/com/mctait/model/`
- [x] Fields: title, companyName, location, salary, summary, url, sourceSite

### 3. Scraper Interface
- [x] `JobScraper.java` in `src/main/java/com/mctait/scraper/`:
```java
public interface JobScraper {
    List<JobPosting> scrape(String keywords, String location, String outputLocation);
    String getSourceName();
}
```
- [x] `AbstractJobScraper` template method class holds the shared behaviour: HTTP calls,
      pagination, retries/backoff, timeouts, filename sanitising and HTML rendering
- [x] `mapJobPosting` is abstract, so a scraper cannot silently inject `null` entries
- [x] Failures surface as `ScrapeException` instead of an empty list

### 4. Scraper Implementations (job board APIs, not HTML scraping)
- [x] `AdzunaScraper` - `https://api.adzuna.com/v1/api/jobs/gb/search/{page}` (HTTPS,
      `results_per_page` capped at the API maximum of 50)
- [x] `ReedScraper` - `https://www.reed.co.uk/api/1.0/search?page={page}&resultsToTake={n}`,
      key sent as a basic-auth header
- [x] Pagination for both sources, stopping on a short page or at `scraping.max-pages`

### 5. Aggregator Service
- [x] `JobAggregatorService` injects all `JobScraper` beans
- [x] Returns an `AggregationResult` (jobs plus the sources that failed) so a broken run is
      distinguishable from an empty search
- [x] One failing source does not abort the others

### 6. CLI Runner
- [x] Accepts `--keywords`, `--location`, `--output-location`, `--help`
- [x] Multi-word values are consumed as a whole (`--keywords Python Developer`)
- [x] Invalid arguments print usage and produce exit code 2
- [x] A scrape failure produces exit code 1; success produces 0

### 7. Output
- [x] One self-contained HTML report per source under `<output.base-path>/<Source>/`
- [x] File names sanitised and validated so they cannot escape the output directory

### 8. Verification
- [x] `./gradlew test` runs the full suite offline (50 tests, 0 failures)
- [x] Manual run with invalid credentials exits non-zero instead of reporting success

---

## Future

- Title/location filter layer and cross-source de-duplication (`ProjectDescription.md`)
- Greenhouse/careers-site scrapers (JSoup or Playwright where HTML parsing is needed)
- JSON/CSV output alongside HTML
- Checkstyle/SpotBugs/Spotless in the CI workflow
- Optional REST API on top of the aggregator
